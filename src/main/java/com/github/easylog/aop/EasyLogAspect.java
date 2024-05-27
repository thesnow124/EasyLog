package com.github.easylog.aop;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.github.easylog.annotation.EasyLog;
import com.github.easylog.compare.Equator;
import com.github.easylog.compare.FieldInfo;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.model.EasyLogOps;
import com.github.easylog.model.MethodExecuteResult;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.service.IOperatorService;
import com.github.easylog.service.OpLogContext;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
        //todo 切面及切面方法执行成功，记录了一条成功日志。但是整体事务回滚了，这个日志就是个错误日志。解决办法：1、（思路不清晰）这个日志，切面所在的事务后提交，事务回滚则记录失败状态；
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
            Map<String, Object> param = buildRequestParam(nameArray, valueArray);
            executeResult.setParam(param);
        } catch (Exception e) {
            log.info("解析通用信息发生异常", e);
        }

        List<EasyLogInfo> easyLogInfoList;
        //方法逻辑
        try {
            OpLogContext.pushLogStack(new ArrayList<>());
            Object result;
            result = joinPoint.proceed();
            executeResult.calcExecuteTime(result);
        } catch (Throwable e) {
            executeResult.exception(e);
        } finally {
            easyLogInfoList = OpLogContext.popLogStack();
            Stack<List<EasyLogInfo>> logStack = OpLogContext.getLogStack();
            if (CollectionUtils.isEmpty(logStack)) {
                OpLogContext.removeLogStack();
            }
        }

        //方法后逻辑
        try {
            Map<String, String> templateMap = easyLogParser.processAfterExec(expressTemplateList, customFunctionExecResultMap, method, args, targetClass, executeResult.getErrMsg(), executeResult.getResult());
            List<EasyLogInfo> easyLogInfos = createEasyLogInfo(templateMap, easyLogOpsList, executeResult, easyLogInfoList);
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
                logRecordService.record(easyLogInfo);
            });
        } catch (Exception e) {
            log.info("方法后逻辑发生异常", e);
        }
        //抛出异常
        if (!executeResult.isSuccess()) {
            throw executeResult.getThrowable();
        }
        return executeResult.getResult();
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


    /**
     * 创建操作日志实体
     *
     * @param templateMap    templateMap
     * @param easyLogOpsList easyLogOpsList
     * @return List<EasyLogInfo>
     */
    private List<EasyLogInfo> createEasyLogInfo(Map<String, String> templateMap, List<EasyLogOps> easyLogOpsList, MethodExecuteResult executeResult,List<EasyLogInfo> easyLogInfoList) {
        List<EasyLogInfo> easyLogInfos = new ArrayList<>();
        for (EasyLogOps easyLogOps : easyLogOpsList) {
            EasyLogInfo easyLogInfo = new EasyLogInfo();
            easyLogInfo.setCondition(templateMap.get(easyLogOps.getCondition()));

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
                    .map(templateMap::get)
                    .toArray(String[]::new);
            easyLogInfo.setContentParam(array);
            if (StringUtils.isNotBlank(easyLogInfo.getDetail())) {
                Object o = null;
                try {
                    o = JSON.parse(easyLogInfo.getDetail());
                } catch (Exception e) {
                    log.info("反序列化失败 easyLogInfo.getDetail()=" + easyLogInfo.getDetail(), e);
                }
                Object oldBean = null;
                //默认为新增
                Object newBean = o;
                if (o instanceof JSONArray) {
                    List<String> list = JSON.parseArray(easyLogOps.getDetails(), String.class);
                    if (!CollectionUtils.isEmpty(list)) {
                        oldBean = list.stream().findFirst().map(JSON::parse).orElse(null);
                        newBean = list.stream().skip(1).findFirst().map(JSON::parse).orElse(null);
                    }
                }
                List<FieldInfo> diffField = Equator.getDiffField(oldBean, newBean);
                easyLogInfo.setFieldInfoList(diffField);
            }
            easyLogInfos.add(easyLogInfo);
        }
        if (!CollectionUtils.isEmpty(easyLogInfoList)) {
            easyLogInfoList.forEach(logInfo -> {
                String platform = templateMap.getOrDefault(logInfo.getPlatform(), operatorService.getPlatform());
                logInfo.setPlatform(platform);
                String operator = templateMap.getOrDefault(logInfo.getOperator(), operatorService.getOperator());
                logInfo.setOperator(operator);
                if (StringUtils.isNotBlank(logInfo.getDetail())) {
                    Object o = null;
                    try {
                        o = JSON.parse(logInfo.getDetail());
                    } catch (Exception e) {
                        log.info("反序列化失败 easyLogInfo.getDetail()=" + logInfo.getDetail(), e);
                    }
                    Object oldBean = null;
                    //默认为新增
                    Object newBean = o;
                    if (o instanceof JSONArray) {
                        List<String> list = JSON.parseArray(logInfo.getDetail(), String.class);
                        if (!CollectionUtils.isEmpty(list)) {
                            oldBean = list.stream().findFirst().map(JSON::parse).orElse(null);
                            newBean = list.stream().skip(1).findFirst().map(JSON::parse).orElse(null);
                        }
                    }
                    List<FieldInfo> diffField = Equator.getDiffField(oldBean, newBean);
                    logInfo.setFieldInfoList(diffField);
                }
                easyLogInfos.add(logInfo);
            });
        }
        return easyLogInfos;
    }

}
