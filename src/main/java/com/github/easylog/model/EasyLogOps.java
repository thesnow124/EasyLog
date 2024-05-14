package com.github.easylog.model;

import lombok.Data;

/**
 * @author Gaosl
 */
@Data
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

    private String details;

    private String condition;

}
