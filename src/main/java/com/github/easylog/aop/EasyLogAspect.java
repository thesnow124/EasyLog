package com.github.easylog.aop;

import com.alibaba.fastjson2.JSON;
import com.github.easylog.annotation.EasyLog;
import com.github.easylog.api.ILogRecordService;
import com.github.easylog.api.IOperatorService;
import com.github.easylog.context.EasyLogContext;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.model.EasyLogOps;
import com.github.easylog.model.MethodExecuteResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 操作日志切面（精简版，逻辑保持不变）。
 */
@Aspect
@Slf4j
public class EasyLogAspect {

    private final ILogRecordService logRecordService;
    private final IOperatorService operatorService;
    private final EasyLogParser easyLogParser;
    private final boolean afterCommit;

    public EasyLogAspect(ILogRecordService logRecordService,
                         IOperatorService operatorService,
                         EasyLogParser easyLogParser,
                         boolean afterCommit) {
        this.logRecordService = logRecordService;
        this.operatorService = operatorService;
        this.easyLogParser = easyLogParser;
        this.afterCommit = afterCommit;
    }

    /** 定义切点 */
    @Pointcut("@annotation(com.github.easylog.annotation.EasyLog) || @annotation(com.github.easylog.annotation.EasyLogs)")
    public void pointCut() {}

    /** 环绕通知 */
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
            List<EasyLogOps> ops = new ArrayList<>();
            EasyLog[] logList = method.getAnnotationsByType(EasyLog.class);
            ops.addAll(Arrays.stream(logList)
                    .map(EasyLogAspectHelper::parseLogAnnotation)
                    .collect(Collectors.toList()));
            easyLogOpsList = ops;
            expressTemplateList = EasyLogAspectHelper.getExpressTemplate(easyLogOpsList);
            customFunctionExecResultMap = easyLogParser.processBeforeExec(expressTemplateList, method, args, targetClass);
        } catch (Exception e) {
            log.info("方法前逻辑发生异常", e);
        }

        // 解析通用信息
        MethodExecuteResult executeResult = new MethodExecuteResult(true);
        try {
            String classMethod = String.format("%s.%s", methodSignature.getDeclaringTypeName(), methodSignature.getName());
            executeResult.setClassMethod(classMethod);
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (Objects.nonNull(attributes)) {
                HttpServletRequest request = attributes.getRequest();
                executeResult.setIp(EasyLogAspectHelper.extractClientIp(request));
                executeResult.setUrl(String.valueOf(request.getRequestURL()));
                executeResult.setHttpMethod(request.getMethod());
            }
            String[] nameArray = methodSignature.getParameterNames();
            Object[] valueArray = joinPoint.getArgs();
            Map<String, Object> param = EasyLogAspectHelper.buildRequestParam(nameArray, valueArray);
            executeResult.setParam(param);
        } catch (Exception e) {
            log.info("解析通用信息发生异常", e);
        }

        // 方法逻辑
        try {
            EasyLogContext.push();
            Object result = joinPoint.proceed();
            executeResult.calcExecuteTime(result);
        } catch (Throwable e) {
            executeResult.exception(e);
        } finally {
            EasyLogContext.pop();
            EasyLogContext.clearIfEmpty();
        }

        // 方法后逻辑
        try {
            Map<String, String> templateMap = easyLogParser.processAfterExec(
                    expressTemplateList, customFunctionExecResultMap, method, args, targetClass,
                    executeResult.getErrMsg(), executeResult.getResult());
            List<EasyLogInfo> easyLogInfos = EasyLogAspectHelper.createEasyLogInfo(
                    templateMap, easyLogOpsList, executeResult, operatorService);
            easyLogInfos.forEach(easyLogInfo -> {
                easyLogInfo.setResult(JSON.toJSONString(executeResult.getResult()));
                easyLogInfo.setSuccess(executeResult.isSuccess());
                if (Objects.nonNull(executeResult.getThrowable())) {
                    easyLogInfo.setStackTrace(ExceptionUtils.getStackTrace(executeResult.getThrowable()));
                }
                easyLogInfo.setErrorMsg(executeResult.getErrMsg());
                easyLogInfo.setExecuteTime(executeResult.getExecuteTime());
                easyLogInfo.setOperateTime(executeResult.getOperateTime());
                easyLogInfo.setIp(executeResult.getIp());
                easyLogInfo.setUrl(executeResult.getUrl());
                easyLogInfo.setHttpMethod(executeResult.getHttpMethod());
                easyLogInfo.setClassMethod(executeResult.getClassMethod());
                easyLogInfo.setParam(executeResult.getParam());
            });
            recordLogs(easyLogInfos);
        } catch (Exception e) {
            log.info("方法后逻辑发生异常", e);
        }

        if (!executeResult.isSuccess()) {
            throw executeResult.getThrowable();
        }
        return executeResult.getResult();
    }

    private void recordLogs(List<EasyLogInfo> logs) {
        if (logs == null || logs.isEmpty()) return;
        if (afterCommit && org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        public void afterCommit() {
                            for (EasyLogInfo logInfo : logs) {
                                try { logRecordService.record(logInfo); } catch (Exception ignore) {}
                            }
                        }
                    }
            );
        } else {
            for (EasyLogInfo logInfo : logs) {
                try { logRecordService.record(logInfo); } catch (Exception ignore) {}
            }
        }
    }
}

