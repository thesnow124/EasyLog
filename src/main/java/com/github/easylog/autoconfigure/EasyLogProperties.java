package com.github.easylog.autoconfigure;

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
     * 日志落地方式：log | jdbc，默认 log
     */
    private String store = "log";

}
