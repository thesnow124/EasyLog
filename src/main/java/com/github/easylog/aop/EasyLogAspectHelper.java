package com.github.easylog.aop;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.github.easylog.annotation.EasyLog;
import com.github.easylog.service.IOperatorService;
import com.github.easylog.compare.Equator;
import com.github.easylog.compare.FieldInfo;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.model.EasyLogOps;
import com.github.easylog.model.MethodExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 提取自 EasyLogAspect 的纯工具逻辑，保持原有实现不变。
 */
@Slf4j
final class EasyLogAspectHelper {
    private EasyLogAspectHelper() {}

    /**
     * 将注解解析为内部操作对象，方便后续统一处理。
     */
    static EasyLogOps parseLogAnnotation(EasyLog easyLog) {
        EasyLogOps easyLogOps = new EasyLogOps();
        easyLogOps.setSuccess(easyLog.success());
        easyLogOps.setSuccessParamList(easyLog.successParamList());
        easyLogOps.setFail(easyLog.fail());
        easyLogOps.setFailParamList(easyLog.failParamList());
        easyLogOps.setModule(easyLog.module());
        easyLogOps.setType(easyLog.type());
        easyLogOps.setOperator(easyLog.operator());
        easyLogOps.setBizNo(easyLog.bizNo());
        easyLogOps.setPlatform(easyLog.platform());
        easyLogOps.setDetails(easyLog.detail());
        easyLogOps.setCondition(easyLog.condition());
        return easyLogOps;
    }

    /**
     * 收集所有需要解析的模板片段（SpEL、自定义函数、条件等），用于统一前/后置渲染。
     */
    static List<String> getExpressTemplate(List<EasyLogOps> easyLogOpsList) {
        Set<String> set = new HashSet<>();
        for (EasyLogOps easyLogOps : easyLogOpsList) {
            set.addAll(java.util.Arrays.asList(
                    easyLogOps.getBizNo(),
                    easyLogOps.getDetails(),
                    easyLogOps.getOperator(),
                    easyLogOps.getPlatform(),
                    easyLogOps.getSuccess(),
                    easyLogOps.getFail(),
                    easyLogOps.getCondition()
            ));
            set.addAll(Arrays.asList(easyLogOps.getSuccessParamList()));
            set.addAll(Arrays.asList(easyLogOps.getFailParamList()));
        }
        return set.stream()
                .filter(s -> !ObjectUtils.isEmpty(s))
                .collect(Collectors.toList());
    }

    /**
     * 构建方法参数快照。MultipartFile/HttpServletRequest 等不可序列化类型会转成可读占位值。
     */
    static Map<String, Object> buildRequestParam(String[] paramNames, Object[] paramValues) {
        Map<String, Object> requestParams = new HashMap<>(16);
        if (paramNames == null) {
            paramNames = new String[paramValues == null ? 0 : paramValues.length];
            for (int i = 0; i < paramNames.length; i++) {
                paramNames[i] = "arg" + i;
            }
        }
        for (int i = 0; i < paramNames.length; i++) {
            Object value = i < paramValues.length ? paramValues[i] : null;
            if (value instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) value;
                value = file.getOriginalFilename();
            }
            if (value instanceof HttpServletRequest) {
                value = "HttpServletRequest can not serializable";
            }
            requestParams.put(paramNames[i], value);
        }
        return requestParams;
    }

    /**
     * 尝试从常见代理头中提取客户端 IP，失败则回落到 remoteAddr。
     */
    static String extractClientIp(HttpServletRequest request) {
        try {
            String xff = request.getHeader("X-Forwarded-For");
            if (!ObjectUtils.isEmpty(xff)) {
                String[] parts = xff.split(",");
                for (String p : parts) {
                    String ip = p.trim();
                    if (!ip.isEmpty()) {
                        return ip;
                    }
                }
            }
            String real = request.getHeader("X-Real-IP");
            if (!ObjectUtils.isEmpty(real)) {
                return real;
            }
        } catch (Exception ignored) {}
        return request.getRemoteAddr();
    }

    /**
     * 根据模板渲染结果生成最终的日志实体列表（成功/失败、占位符、条件过滤、差异计算）。
     */
    static List<EasyLogInfo> createEasyLogInfo(Map<String, String> templateMap,
                                               List<EasyLogOps> easyLogOpsList,
                                               MethodExecuteResult executeResult,
                                               IOperatorService operatorService) {
        List<EasyLogInfo> easyLogInfos = new ArrayList<>();
        for (EasyLogOps easyLogOps : easyLogOpsList) {
            boolean shouldRecord = true;
            String conditionKey = easyLogOps.getCondition();
            if (!ObjectUtils.isEmpty(conditionKey)) {
                String condVal = templateMap.get(conditionKey);
                shouldRecord = Boolean.parseBoolean(String.valueOf(condVal));
            }
            if (!shouldRecord) {
                continue;
            }
            // 基础信息：是否记录、操作人/平台、模块、类型、业务标识、详情等
            EasyLogInfo easyLogInfo = new EasyLogInfo();
            easyLogInfo.setCondition(ObjectUtils.isEmpty(conditionKey) ? "true" : templateMap.get(conditionKey));
            String platform = templateMap.getOrDefault(easyLogOps.getPlatform(), operatorService.getPlatform());
            easyLogInfo.setPlatform(platform);
            String operator = templateMap.getOrDefault(easyLogOps.getOperator(), operatorService.getOperator());
            easyLogInfo.setOperator(operator);

            easyLogInfo.setModule(easyLogOps.getModule());
            easyLogInfo.setType(easyLogOps.getType());
            easyLogInfo.setBizNo(templateMap.get(easyLogOps.getBizNo()));
            easyLogInfo.setDetail(templateMap.get(easyLogOps.getDetails()));
            String contentKey = easyLogOps.getSuccess();
            String[] paramKeyList = easyLogOps.getSuccessParamList();
            if (!executeResult.isSuccess()) {
                // 执行失败时切换到 fail 模板及其参数列表
                contentKey = easyLogOps.getFail();
                paramKeyList = easyLogOps.getFailParamList();
            }
            easyLogInfo.setContent(templateMap.get(contentKey));
            // 渲染内容参数，缺失时回退为空串以防 NPE
            String[] array = Arrays.stream(paramKeyList)
                    .map(k -> {
                        String v = templateMap.get(k);
                        return v == null ? "" : v;
                    })
                    .toArray(String[]::new);
            easyLogInfo.setContentParam(array);
            // 差异详情：解析 JSON，生成字段级差异列表
            easyLogInfo.setFieldInfoList(getFieldInfoList(easyLogInfo.getDetail()));
            easyLogInfos.add(easyLogInfo);
        }
        return easyLogInfos;
    }

    private static List<FieldInfo> getFieldInfoList(String detail) {
        Object o;
        try {
            o = JSON.parse(detail);
        } catch (Exception e) {
            log.info("反序列化失败 detail=" + detail, e);
            FieldInfo fieldDiff = new FieldInfo();
            fieldDiff.setVal(detail);
            return Collections.singletonList(fieldDiff);
        }
        // 支持 detail=[old,new,...] 形式的 JSON 数组，取第一个元素为旧值，最后一个元素为新值
        String oldBean = null;
        String newBean = detail;
        if (o instanceof JSONArray) {
            List<String> list = JSON.parseArray(detail, String.class);
            if (CollectionUtils.isEmpty(list)) {
                return Collections.emptyList();
            }
            oldBean = list.get(0);
            newBean = list.size() > 1 ? list.get(list.size() - 1) : null;
        } else {
            FieldInfo fieldDiff = new FieldInfo();
            fieldDiff.setVal(detail);
            return Collections.singletonList(fieldDiff);
        }
        return Equator.getDiffField(oldBean, newBean);
    }
}
