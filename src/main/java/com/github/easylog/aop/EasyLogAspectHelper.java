package com.github.easylog.aop;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.github.easylog.annotation.EasyLog;
import com.github.easylog.api.IOperatorService;
import com.github.easylog.compare.Equator;
import com.github.easylog.compare.FieldInfo;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.model.EasyLogOps;
import com.github.easylog.model.MethodExecuteResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

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

    static Map<String, Object> buildRequestParam(String[] paramNames, Object[] paramValues) {
        Map<String, Object> requestParams = new HashMap<>(16);
        if (paramNames == null) {
            paramNames = new String[paramValues == null ? 0 : paramValues.length];
            for (int i = 0; i < paramNames.length; i++) paramNames[i] = "arg" + i;
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

    static String extractClientIp(HttpServletRequest request) {
        try {
            String xff = request.getHeader("X-Forwarded-For");
            if (!ObjectUtils.isEmpty(xff)) {
                String[] parts = xff.split(",");
                for (String p : parts) {
                    String ip = p.trim();
                    if (!ip.isEmpty()) return ip;
                }
            }
            String real = request.getHeader("X-Real-IP");
            if (!ObjectUtils.isEmpty(real)) return real;
        } catch (Exception ignored) {}
        return request.getRemoteAddr();
    }

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
                contentKey = easyLogOps.getFail();
                paramKeyList = easyLogOps.getFailParamList();
            }
            easyLogInfo.setContent(templateMap.get(contentKey));
            String[] array = Arrays.stream(paramKeyList)
                    .map(k -> {
                        String v = templateMap.get(k);
                        return v == null ? "" : v;
                    })
                    .toArray(String[]::new);
            easyLogInfo.setContentParam(array);
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
        String oldBean = null;
        String newBean = detail;
        if (o instanceof JSONArray) {
            List<String> list = JSON.parseArray(detail, String.class);
            if (!CollectionUtils.isEmpty(list)) {
                oldBean = list.stream().findFirst().orElse(null);
                newBean = list.stream().skip(1).findFirst().orElse(null);
            }
        } else {
            FieldInfo fieldDiff = new FieldInfo();
            fieldDiff.setVal(detail);
            return Collections.singletonList(fieldDiff);
        }
        return Equator.getDiffField(oldBean, newBean);
    }
}

