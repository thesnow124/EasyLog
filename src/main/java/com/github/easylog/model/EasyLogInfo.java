package com.github.easylog.model;


import com.github.easylog.diff.DiffDTO;

import java.util.Map;

/**
 * @author Gaosl
 */
public class EasyLogInfo {

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


    /**
     * 平台
     */
    private String platform;

    /**
     * 操作者
     */
    private String operator;

    /**
     * 操作时间 时间戳单位：ms
     */
    private Long operateTime;

    /**
     * 业务id
     */
    private String bizNo;

    /**
     * 模块
     */
    private String module;

    /**
     * 操作类型
     */
    private String type;

    /**
     * 操作内容
     */
    private String content;

    /**
     * 操作参数
     */
    private String[] contentParam;

    /**
     * 操作花费的时间 单位：ms
     */
    private Long executeTime;

    /**
     * 是否调用成功
     */
    private Boolean success;

    /**
     * 执行后返回的json字符串
     */
    private String result;

    private String errorMsg;

    /**
     * 异常堆栈信息
     */
    private String stackTrace;


    /**
     * 额外扩展信息（不参与字段差异对比）
     */
    private String extra;

    /**
     * 结构化Diff明细
     */
    private DiffDTO diffDTO;

    /**
     * 记录条件
     */
    private String condition;

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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Long getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(Long operateTime) {
        this.operateTime = operateTime;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String[] getContentParam() {
        return contentParam;
    }

    public void setContentParam(String[] contentParam) {
        this.contentParam = contentParam;
    }

    public Long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public DiffDTO getDiffDTO() {
        return diffDTO;
    }

    public void setDiffDTO(DiffDTO diffDTO) {
        this.diffDTO = diffDTO;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
