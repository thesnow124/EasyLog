package com.github.easylog.support;


import com.github.easylog.service.IOperatorService;
import com.github.easylog.configuration.EasyLogProperties;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Gaosl
 */
public class DefaultOperatorServiceImpl implements IOperatorService {

    private final EasyLogProperties properties;

    public DefaultOperatorServiceImpl(EasyLogProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getOperator() {
        return "unknown";
    }

    @Override
    public String getPlatform() {
        String platform = properties.getPlatform();
        return StringUtils.isNotBlank(platform) ? platform : "unknown";
    }
}
