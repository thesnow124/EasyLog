package com.github.easylog.model;


/**
 * @author Gaosl
 */
public class EasyLogOps {

    private String platform;

    private String operator;

    private String bizNo;

    private String module;

    private String type;

    private String success;

    /**
     * 成功参数
     */
    private String[] successParamList;

    private String fail;

    /**
     * 失败参数
     */
    private String[] failParamList;

    private String diffKey;

    private String extra;

    private String condition;

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

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String[] getSuccessParamList() {
        return successParamList;
    }

    public void setSuccessParamList(String[] successParamList) {
        this.successParamList = successParamList;
    }

    public String getFail() {
        return fail;
    }

    public void setFail(String fail) {
        this.fail = fail;
    }

    public String[] getFailParamList() {
        return failParamList;
    }

    public void setFailParamList(String[] failParamList) {
        this.failParamList = failParamList;
    }

    public String getDiffKey() {
        return diffKey;
    }

    public void setDiffKey(String diffKey) {
        this.diffKey = diffKey;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
