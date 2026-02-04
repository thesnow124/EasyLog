package com.github.easylog.aop;

import com.alibaba.fastjson2.JSON;
import com.github.easylog.annotation.EasyLog;
import com.github.easylog.context.EasyLogContext;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.model.EasyLogOps;
import com.github.easylog.model.MethodExecuteResult;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.service.IOperatorService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 操作日志切面（精简版，逻辑保持不变）。
 * <p>
 * 核心职责：
 * <ol>
 *     <li>在方法执行前解析注解与模板，预先计算需要前置执行的自定义函数（如“查询旧值”）。</li>
 *     <li>执行业务方法，捕获返回结果或异常，同时记录耗时与请求上下文。</li>
 *     <li>在方法执行后渲染模板（成功/失败、SpEL、自定义函数、占位符），构造 {@link EasyLogInfo} 并交给存储层。</li>
 * </ol>
 * 任何解析/存储异常都被吞掉以避免影响业务主流程。
 * @author gaoshuanglong
 */
@Aspect
public class EasyLogAspect {

    private static final Logger LOG = Logger.getLogger(EasyLogAspect.class.getName());

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

        // 方法前逻辑：解析注解、提取模板、执行前置函数
        List<String> expressTemplateList = new ArrayList<>();
        Map<String, String> customFunctionExecResultMap = new HashMap<>();
        List<EasyLogOps> easyLogOpsList = new ArrayList<>();
        try {
            EasyLog[] logList = method.getAnnotationsByType(EasyLog.class);
            easyLogOpsList = Arrays.stream(logList)
                    .map(EasyLogAspectHelper::parseLogAnnotation).collect(Collectors.toList());
            expressTemplateList = EasyLogAspectHelper.getExpressTemplate(easyLogOpsList);
            customFunctionExecResultMap = easyLogParser.processBeforeExec(expressTemplateList, method, args, targetClass);
        } catch (Exception e) {
            LOG.log(Level.INFO, "方法前逻辑发生异常", e);
        }

        // 解析通用信息：请求元数据、参数快照、类名+方法名
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
            LOG.log(Level.INFO, "解析通用信息发生异常", e);
        }

        // 方法逻辑：执行业务方法并捕获结果/异常；使用上下文栈隔离嵌套调用
        Map<String, Object> localVars = null;
        try {
            EasyLogContext.push();
            Object result = joinPoint.proceed();
            executeResult.calcExecuteTime(result);
        } catch (Throwable e) {
            executeResult.exception(e);
        } finally {
            localVars = EasyLogContext.pop();
            EasyLogContext.clearIfEmpty();
        }
        if (localVars == null) {
            localVars = new HashMap<>();
        }

        // 方法后逻辑：渲染模板 -> 生成日志 -> 落地
        try {
            Map<String, String> templateMap = easyLogParser.processAfterExec(
                    expressTemplateList, customFunctionExecResultMap, method, args, targetClass,
                    executeResult.getErrMsg(), executeResult.getResult(), localVars);

            List<EasyLogInfo> easyLogInfos = EasyLogAspectHelper.createEasyLogInfo(
                    templateMap, easyLogOpsList, executeResult, operatorService);
            easyLogInfos.forEach(easyLogInfo -> {
                easyLogInfo.setResult(JSON.toJSONString(executeResult.getResult()));
                easyLogInfo.setSuccess(executeResult.isSuccess());
                if (Objects.nonNull(executeResult.getThrowable())) {
                    easyLogInfo.setStackTrace(getStackTrace(executeResult.getThrowable()));
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
            LOG.log(Level.INFO, "方法后逻辑发生异常", e);
        }

        if (!executeResult.isSuccess()) {
            throw executeResult.getThrowable();
        }
        return executeResult.getResult();
    }


    private static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private void recordLogs(List<EasyLogInfo> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        if (afterCommit && org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
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
