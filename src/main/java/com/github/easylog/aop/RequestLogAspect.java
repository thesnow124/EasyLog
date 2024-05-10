package com.github.easylog.aop;

import com.github.easylog.service.IOperatorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @ClassDescription: web层切面逻辑
 * @author: long.gao
 * @date: 2023.09.14
 */
@Component
@Aspect
@Slf4j
public class RequestLogAspect {

    @Resource
    private IOperatorService operatorService;

    @Pointcut("@annotation(com.github.easylog.annotation.EasyLog) || @annotation(com.github.easylog.annotation.EasyLogs)")
    public void requestLogPointCut() {
    }

//    @Around("requestLogPointCut()")
//    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
//        RequestLogInfo requestLogInfo = new RequestLogInfo();
//        long start = System.currentTimeMillis();
//        Object result;
//        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
//        Method method = signature.getMethod();
//        EasyLog requestLog = method.getDeclaredAnnotation(EasyLog.class);
//        try {
//            HttpServletRequest request = null;
//            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//            if (Objects.nonNull(attributes)) {
//                request = attributes.getRequest();
//            }
//            String classMethod = String.format("%s.%s", signature.getDeclaringTypeName(), signature.getName());
//            //参数名
//            String[] nameArray = signature.getParameterNames();
//            //参数值
//            Object[] valueArray = proceedingJoinPoint.getArgs();
//            if (Objects.nonNull(request)) {
//                requestLogInfo.setIp(request.getRemoteAddr());
//                requestLogInfo.setUrl(String.valueOf(request.getRequestURL()));
//                requestLogInfo.setHttpMethod(request.getMethod());
//            }
//
//            requestLogInfo.setClassMethod(classMethod);
//            requestLogInfo.setParamMap(buildRequestParam(nameArray, valueArray));
//            requestLogInfo.setUserName(operatorService.getOperator());
//            requestLogInfo.setUserCode(operatorService.getOperator());
//            log.info("RequestLog | start | requestInfo={}", JsonUtils.toJSONString(requestLogInfo));
//        } catch (Exception e) {
//            log.info("RequestLog | start | error | requestInfo" + JsonUtils.toJSONString(requestLogInfo), e);
//        }
//
//        result = proceedingJoinPoint.proceed();
//
//        try {
//            requestLogInfo.setParamMap(null);
//            if (Objects.nonNull(requestLog) &&  requestLog.isPrintResult()) {
//                requestLogInfo.setResult(result);
//            }
//            requestLogInfo.setTimeCost(System.currentTimeMillis() - start);
//            log.info("RequestLog | end | requestInfo={}", JsonUtils.toJSONString(requestLogInfo));
//        } catch (Exception e) {
//            log.info("RequestLog | end | error | requestInfo" + JsonUtils.toJSONString(requestLogInfo), e);
//        }
//        return result;
//    }
//
//    private Map<String, Object> buildRequestParam(String[] paramNames, Object[] paramValues) {
//        Map<String, Object> requestParams = new HashMap<>(16);
//        for (int i = 0; i < paramNames.length; i++) {
//            Object value = paramValues[i];
//            //如果是文件对象
//            if (value instanceof MultipartFile) {
//                MultipartFile file = (MultipartFile) value;
//                value = file.getOriginalFilename();
//            }
//
//            requestParams.put(paramNames[i], value);
//        }
//        return requestParams;
//    }

    @Data
    public static class RequestLogInfo {

        /**
         * ip
         */
        private String ip;

        /**
         * url
         */
        private String url;

        /**
         * HTTP请求方式
         */
        private String httpMethod;

        /**
         * 类.方法
         */
        private String classMethod;

        /**
         * 操作人账号
         */
        private String userCode;

        /**
         * 操作人姓名
         */
        private String userName;

        /**
         * 接口参数
         */
        private Map<String, Object> paramMap;

        /**
         * 接口结果
         */
        private Object result;

        /**
         * 接口耗时
         */
        private Long timeCost;

    }

}

