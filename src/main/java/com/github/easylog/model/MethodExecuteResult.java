package com.github.easylog.model;

import java.util.Map;

/**
 * @author Gaosl
 */
public class MethodExecuteResult {

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
     * 接口参数
     */
    private Map<String, Object> param;


    private boolean success;

    private Throwable throwable;

    private String errMsg;

    private Long operateTime;

    private Long executeTime;

    private Object result;

    public MethodExecuteResult(boolean success) {
        this.success = success;
        this.operateTime = System.currentTimeMillis();
    }

    public void calcExecuteTime(Object result) {
        this.executeTime = System.currentTimeMillis() - this.operateTime;
        this.result = result;
    }

    public void exception(Throwable throwable) {
        this.success = false;
        this.executeTime = System.currentTimeMillis() - this.operateTime;
        this.throwable = throwable;
        this.errMsg = throwable.getMessage();
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getClassMethod() {
        return classMethod;
    }

    public void setClassMethod(String classMethod) {
        this.classMethod = classMethod;
    }

    public Map<String, Object> getParam() {
        return param;
    }

    public void setParam(Map<String, Object> param) {
        this.param = param;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public Long getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(Long operateTime) {
        this.operateTime = operateTime;
    }

    public Long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
