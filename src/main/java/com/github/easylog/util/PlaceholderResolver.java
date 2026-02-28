package com.github.easylog.util;

import org.springframework.util.PropertyPlaceholderHelper;

import java.util.stream.Stream;

/**
 * @author Gaosl
 */
public class PlaceholderResolver {
    /**
     * 默认前缀占位符
     */
    private static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

    /**
     * 默认后缀占位符
     */
    private static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    /**
     * 默认单例解析器
     */
    private static final PlaceholderResolver DEFAULT_RESOLVER = new PlaceholderResolver();

    /**
     * 占位符前缀
     */
    private final String placeholderPrefix;

    /**
     * 占位符后缀
     */
    private final String placeholderSuffix;

    private final PropertyPlaceholderHelper placeholderHelper;


    private PlaceholderResolver() {
        this(DEFAULT_PLACEHOLDER_PREFIX, DEFAULT_PLACEHOLDER_SUFFIX);
    }

    private PlaceholderResolver(String placeholderPrefix, String placeholderSuffix) {
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        this.placeholderHelper = new PropertyPlaceholderHelper(placeholderPrefix, placeholderSuffix, null, true);
    }

    /**
     * 获取默认的占位符解析器，即占位符前缀为"${", 后缀为"}"
     *
     * @return
     */
    public static PlaceholderResolver getDefaultResolver() {
        return DEFAULT_RESOLVER;
    }

    public static PlaceholderResolver getResolver(String placeholderPrefix, String placeholderSuffix) {
        return new PlaceholderResolver(placeholderPrefix, placeholderSuffix);
    }

    /**
     * 解析带有指定占位符的模板字符串，默认占位符为前缀：${  后缀：}<br/><br/>
     * 如：template = category:${}:product:${}<br/>
     * values = {"1", "2"}<br/>
     * 返回 category:1:product:2<br/>
     *
     * @param content 要解析的带有占位符的模板字符串
     * @param values  按照模板占位符索引位置设置对应的值
     * @return
     */
    public String resolve(String content, String[] values) {
        if (isBlank(content) || values == null) {
            return content;
        }
        if (!containsPlaceholder(content)) {
            return content;
        }
        int[] valueIndex = {0};
        return placeholderHelper.replacePlaceholders(content, placeholder -> {
            int replaceIndex = resolveReplaceIndex(placeholder, valueIndex, values.length);
            if (replaceIndex < 0) {
                return null;
            }
            return values[replaceIndex];
        });
    }

    /**
     * 解析带有指定占位符的模板字符串，默认占位符为前缀：${  后缀：}<br/><br/>
     * 如：template = category:${}:product:${}<br/>
     * values = {"1", "2"}<br/>
     * 返回 category:1:product:2<br/>
     *
     * @param content 要解析的带有占位符的模板字符串
     * @param values  按照模板占位符索引位置设置对应的值
     * @return
     */
    public String resolve(String content, Object... values) {
        return resolve(content, Stream.of(values).map(String::valueOf).toArray(String[]::new));
    }

    private static boolean isBlank(String value) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNumeric(String value) {
        if (isBlank(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }




    private boolean containsPlaceholder(String content) {
        return content.contains(this.placeholderPrefix);
    }

    private static int resolveReplaceIndex(String placeholder, int[] valueIndex, int valueCount) {
        if (placeholder == null) {
            return -1;
        }
        if (placeholder.trim().isEmpty()) {
            int current = valueIndex[0];
            if (current >= valueCount) {
                return -1;
            }
            valueIndex[0] = current + 1;
            return current;
        }
        if (isNumeric(placeholder)) {
            int index = Integer.parseInt(placeholder);
            if (index >= 0 && index < valueCount) {
                return index;
            }
        }
        return -1;
    }

}
