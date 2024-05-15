package com.github.easylog.aop;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.compare.Equator;
import com.github.easylog.compare.FieldInfo;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.model.EasyLogOps;
import com.github.easylog.model.MethodExecuteResult;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.service.IOperatorService;
import com.github.easylog.util.JsonUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Gaosl
 */
@Aspect
@Component
@Slf4j
public class EasyLogAspect {

    private ILogRecordService logRecordService;

    private IOperatorService operatorService;

    private EasyLogParser easyLogParser;


    public EasyLogAspect(ILogRecordService logRecordService, IOperatorService operatorService, EasyLogParser easyLogParser) {
        this.logRecordService = logRecordService;
        this.operatorService = operatorService;
        this.easyLogParser = easyLogParser;
    }

    /**
     * 定义切点
     */
    @Pointcut("@annotation(com.github.easylog.annotation.EasyLog) || @annotation(com.github.easylog.annotation.EasyLogs)")
    public void pointCut() {
    }

    /**
     * 环绕通知
     *
     * @param joinPoint joinPoint
     * @return Object
     */
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = joinPoint.getArgs();
        Object target = joinPoint.getTarget();
        Class<?> targetClass = AopUtils.getTargetClass(target);

        // 方法前逻辑
        List<String> expressTemplateList = new ArrayList<>();
        Map<String, String> customFunctionExecResultMap = new HashMap<>();
        List<EasyLogOps> easyLogOpsList = new ArrayList<>();
        try {
            EasyLog[] logList = method.getAnnotationsByType(EasyLog.class);
            easyLogOpsList = Arrays.stream(logList)
                    .map(this::parseLogAnnotation)
                    .collect(Collectors.toList());
            expressTemplateList = getExpressTemplate(easyLogOpsList);
            customFunctionExecResultMap = easyLogParser.processBeforeExec(expressTemplateList, method, args, targetClass);
        } catch (Exception e) {
            log.info("方法前逻辑发生异常", e);
        }

        //解析通用信息
        MethodExecuteResult executeResult = new MethodExecuteResult(true);
        try {
            String classMethod = String.format("%s.%s", methodSignature.getDeclaringTypeName(), methodSignature.getName());
            executeResult.setClassMethod(classMethod);
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (Objects.nonNull(attributes)) {
                HttpServletRequest request = attributes.getRequest();
                executeResult.setIp(request.getRemoteAddr());
                executeResult.setUrl(String.valueOf(request.getRequestURL()));
                executeResult.setHttpMethod(request.getMethod());
            }
            String[] nameArray = methodSignature.getParameterNames();
            Object[] valueArray = joinPoint.getArgs();
            Map<String, Object> paramMap = buildRequestParam(nameArray, valueArray);
            executeResult.setParamMap(paramMap);
        } catch (Exception e) {
            log.info("解析通用信息发生异常", e);
        }

        //方法逻辑
        Object result = null;
        try {
            result = joinPoint.proceed();
            executeResult.calcExecuteTime();
        } catch (Throwable e) {
            executeResult.exception(e);
        }

        //方法后逻辑
        try {
            //todo 默认顺序，提供异步的口子（复制threadLocal）
            after(easyLogOpsList, executeResult, method, expressTemplateList, customFunctionExecResultMap, args, targetClass, result);
        } catch (Exception e) {
            log.info("方法后逻辑发生异常", e);
        }
        //抛出异常
        if (!executeResult.isSuccess()) {
            throw executeResult.getThrowable();
        }
        return result;
    }

    private void after(List<EasyLogOps> easyLogOpsList, MethodExecuteResult executeResult, Method method, List<String> expressTemplate, Map<String, String> customFunctionExecResultMap, Object[] args, Class<?> targetClass, Object result) {
        boolean existsNoFailTemp = easyLogOpsList.stream()
                .anyMatch(easyLogOps -> ObjectUtils.isEmpty(easyLogOps.getFail()));
        if (!executeResult.isSuccess() && existsNoFailTemp) {
            log.warn("[{}] 方法执行失败，EasyLog 失败模板没有配置", method.getName());
        } else {
            Map<String, String> templateMap = easyLogParser.processAfterExec(expressTemplate, customFunctionExecResultMap, method, args, targetClass, executeResult.getErrMsg(), result);
            sendLog(easyLogOpsList, result, executeResult, templateMap);
        }
    }

    /**
     * 发送日志
     *
     * @param easyLogOps    easyLogOps
     * @param result        result
     * @param executeResult executeResult
     * @param templateMap   templateMap
     */
    private void sendLog(List<EasyLogOps> easyLogOps, Object result, MethodExecuteResult executeResult, Map<String, String> templateMap) {
        List<EasyLogInfo> easyLogInfos = createEasyLogInfo(templateMap, easyLogOps, executeResult);
        if (!CollectionUtils.isEmpty(easyLogInfos)) {
            easyLogInfos.forEach(easyLogInfo -> {
                easyLogInfo.setResult(JsonUtils.toJSONString(result));
                easyLogInfo.setSuccess(executeResult.isSuccess());
                easyLogInfo.setErrorMsg(executeResult.getErrMsg());
                easyLogInfo.setExecuteTime(executeResult.getExecuteTime());
                easyLogInfo.setOperateTime(executeResult.getOperateTime());
                easyLogInfo.setIp(executeResult.getIp());
                easyLogInfo.setUrl(executeResult.getUrl());
                easyLogInfo.setHttpMethod(executeResult.getHttpMethod());
                easyLogInfo.setClassMethod(executeResult.getClassMethod());
                logRecordService.record(easyLogInfo);
            });
        }
    }

    private Map<String, Object> buildRequestParam(String[] paramNames, Object[] paramValues) {
        Map<String, Object> requestParams = new HashMap<>(16);
        for (int i = 0; i < paramNames.length; i++) {
            Object value = paramValues[i];
            //如果是文件对象
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
     * 创建操作日志实体
     *
     * @param templateMap    templateMap
     * @param easyLogOpsList easyLogOpsList
     * @return List<EasyLogInfo>
     */
    private List<EasyLogInfo> createEasyLogInfo(Map<String, String> templateMap, List<EasyLogOps> easyLogOpsList, MethodExecuteResult executeResult) {
        List<EasyLogInfo> easyLogInfos = new ArrayList<>();
        for (EasyLogOps easyLogOps : easyLogOpsList) {
            //记录条件为 false，则不记录
            if (Boolean.FALSE.toString().equalsIgnoreCase(templateMap.get(easyLogOps.getCondition()))) {
                continue;
            }
            EasyLogInfo easyLogInfo = new EasyLogInfo();
            String platform = templateMap.get(easyLogOps.getPlatform());
            if (ObjectUtils.isEmpty(platform)) {
                platform = operatorService.getPlatform();
            }
            easyLogInfo.setPlatform(platform);
            String operator = templateMap.get(easyLogOps.getOperator());
            if (ObjectUtils.isEmpty(operator)) {
                operator = operatorService.getOperator();
            }
            easyLogInfo.setModule(easyLogOps.getModule());
            easyLogInfo.setType(easyLogOps.getType());
            easyLogInfo.setOperator(operator);
            easyLogInfo.setBizNo(templateMap.get(easyLogOps.getBizNo()));
            easyLogInfo.setDetails(templateMap.get(easyLogOps.getDetails()));
            String contentKey = easyLogOps.getSuccess();
            String[] paramKeyList = easyLogOps.getSuccessParamList();
            //todo 如果是失败，应该无视内容，把失败模板传进去
            if (!executeResult.isSuccess()) {
                contentKey = easyLogOps.getFail();
                paramKeyList = easyLogOps.getFailParamList();
            }
            easyLogInfo.setContent(templateMap.get(contentKey));
            String[] array = Arrays.stream(paramKeyList)
                    .map(templateMap::get)
                    .toArray(String[]::new);
            easyLogInfo.setParamList(array);
            List<?> list = JsonUtils.toObject(easyLogOps.getDetails(), List.class);
            if (!CollectionUtils.isEmpty(list)) {
                Object oldBean = list.stream().findFirst().orElse(null);
                Object newBean = list.stream().skip(1).findFirst().orElse(null);
                List<FieldInfo> diffField = Equator.getDiffField(oldBean, newBean);
                easyLogInfo.setFieldInfoList(diffField);
            }
            easyLogInfos.add(easyLogInfo);
        }

        return easyLogInfos;
    }


    /**
     * 将注解转为实体
     *
     * @param easyLog easyLog
     * @return EasyLogOps
     */
    private EasyLogOps parseLogAnnotation(EasyLog easyLog) {
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
     * 获取不为空的 待解析模板
     *
     * @param easyLogOpsList easyLogOpsList
     * @return List<String>
     */
    private List<String> getExpressTemplate(List<EasyLogOps> easyLogOpsList) {
        Set<String> set = new HashSet<>();
        for (EasyLogOps easyLogOps : easyLogOpsList) {
            set.addAll(Lists.newArrayList(easyLogOps.getBizNo(), easyLogOps.getDetails(),
                    easyLogOps.getOperator(), easyLogOps.getPlatform(), easyLogOps.getSuccess(), easyLogOps.getFail(),
                    easyLogOps.getCondition()));
            set.addAll(Arrays.asList(easyLogOps.getSuccessParamList()));
            set.addAll(Arrays.asList(easyLogOps.getFailParamList()));
        }
        return set.stream()
                .filter(s -> !ObjectUtils.isEmpty(s))
                .collect(Collectors.toList());
    }

}
