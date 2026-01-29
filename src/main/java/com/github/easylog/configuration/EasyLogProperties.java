package com.github.easylog.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * @author Gaosl
 */
@Setter
@Getter
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

}
