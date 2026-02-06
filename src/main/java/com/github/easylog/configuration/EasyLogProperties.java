package com.github.easylog.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * @author Gaosl
 */
@ConfigurationProperties(prefix = "easylog")
public class EasyLogProperties {
    /**
     * 是否开启操作日志，默认开启
     */
    private boolean enable = true;

    /**
     * 平台：不同服务使用的区分，默认取 spring.application.name
     */
    @Value("${spring.application.name:#{null}}")
    private String platform;

    /**
     * 日志落地方式：log（默认）。如需其它方式请自定义 ILogRecordService。
     */
    private String store = "log";

    /**
     * 是否在事务提交后再落地日志（存在事务时有效）。
     * 默认为 false（方法结束即记录）。
     */
    private boolean afterCommit = false;

    /**
     * Diff时是否忽略旧对象为null的字段
     */
    private boolean diffIgnoreOldObjectNullValue = false;

    /**
     * Diff时是否忽略新对象为null的字段
     */
    private boolean diffIgnoreNewObjectNullValue = false;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public boolean isAfterCommit() {
        return afterCommit;
    }

    public void setAfterCommit(boolean afterCommit) {
        this.afterCommit = afterCommit;
    }

    public boolean isDiffIgnoreOldObjectNullValue() {
        return diffIgnoreOldObjectNullValue;
    }

    public void setDiffIgnoreOldObjectNullValue(boolean diffIgnoreOldObjectNullValue) {
        this.diffIgnoreOldObjectNullValue = diffIgnoreOldObjectNullValue;
    }

    public boolean isDiffIgnoreNewObjectNullValue() {
        return diffIgnoreNewObjectNullValue;
    }

    public void setDiffIgnoreNewObjectNullValue(boolean diffIgnoreNewObjectNullValue) {
        this.diffIgnoreNewObjectNullValue = diffIgnoreNewObjectNullValue;
    }
}
