package com.github.easylog.support;


import com.github.easylog.configuration.EasyLogProperties;
import com.github.easylog.service.IOperatorService;

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
        return isNotBlank(platform) ? platform : "unknown";
    }

    private static boolean isNotBlank(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}

