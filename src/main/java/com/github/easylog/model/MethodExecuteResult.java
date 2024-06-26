package com.github.easylog.model;

import lombok.Data;

import java.util.Map;

/**
 * @author Gaosl
 */
@Data
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


}
