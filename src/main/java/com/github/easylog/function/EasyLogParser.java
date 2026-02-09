package com.github.easylog.function;

import com.alibaba.fastjson2.JSON;
import com.github.easylog.constants.EasyLogConsts;
import com.github.easylog.context.EasyLogCachedExpressionEvaluator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 核心模板解析器，仅支持 SpEL 模板：
 * <ul>
 *     <li>SpEL 块：<code>{{ expression }}</code>。</li>
 *     <li>纯表达式：<code>#arg</code>、<code>@bean</code>、<code>T(xxx)</code>。</li>
 * </ul>
 * 函数统一通过 SpEL 方法调用进入 {@link IParseFunction} 调度链。
 *
 * @author gaoshuanglong
 */
public class EasyLogParser implements BeanFactoryAware {

    private static final Logger LOG = Logger.getLogger(EasyLogParser.class.getName());
    private static final Pattern SPEL_BLOCK = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");

    private BeanFactory beanFactory;
    private final EasyLogCachedExpressionEvaluator cachedExpressionEvaluator;

    public EasyLogParser() {
        this(null);
    }

    public EasyLogParser(IFunctionService functionService) {
        this.cachedExpressionEvaluator = new EasyLogCachedExpressionEvaluator(functionService);
    }

    /**
     * After method execution: render all templates.
     */
    public Map<String, Object> processAfterExec(List<String> expressTemplate,
                                                Map<String, Object> beforeCache,
                                                Method method,
                                                Object[] args,
                                                Class<?> targetClass,
                                                String errMsg,
                                                Object result) {
        return processAfterExec(expressTemplate, beforeCache, method, args, targetClass, errMsg, result, null);
    }

    /**
     * After method execution: render all templates with local variables from EasyLogContext.
     */
    public Map<String, Object> processAfterExec(List<String> expressTemplate,
                                                Map<String, Object> beforeCache,
                                                Method method,
                                                Object[] args,
                                                Class<?> targetClass,
                                                String errMsg,
                                                Object result,
                                                Map<String, Object> localVars) {
        HashMap<String, Object> map = new HashMap<>();
        if (CollectionUtils.isEmpty(expressTemplate)) {
            return map;
        }
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, targetClass);
        EvaluationContext ctx = cachedExpressionEvaluator.createEvaluationContext(method, args, beanFactory, errMsg, result, localVars);
        for (String template : expressTemplate) {
            Object resolved = resolveTemplate(template, elementKey, ctx);
            map.put(template, resolved);
        }
        return map;
    }

    /**
     * Before method execution: reserved no-op.
     */
    public Map<String, Object> processBeforeExec(List<String> templates,
                                                 Method method,
                                                 Object[] args,
                                                 Class<?> targetClass) {
        return new HashMap<>();
    }

    private Object resolveTemplate(String template,
                                   AnnotatedElementKey elementKey,
                                   EvaluationContext ctx) {
        if (template == null) {
            return null;
        }
        String trimmed = template.trim();
        if (isSingleSpelBlock(trimmed)) {
            Matcher matcher = SPEL_BLOCK.matcher(trimmed);
            if (matcher.matches()) {
                return safeEval(matcher.group(1), elementKey, ctx);
            }
        }
        Matcher spelMatcher = SPEL_BLOCK.matcher(template);
        StringBuffer spelBuf = new StringBuffer();
        boolean spelMatched = false;
        while (spelMatcher.find()) {
            spelMatched = true;
            String expr = spelMatcher.group(1);
            String replacement = stringVal(safeEval(expr, elementKey, ctx));
            spelMatcher.appendReplacement(spelBuf, Matcher.quoteReplacement(replacement));
        }
        spelMatcher.appendTail(spelBuf);
        String replaced = spelBuf.toString();
        if (!spelMatched && isPlainExpression(replaced)) {
            return safeEval(replaced, elementKey, ctx);
        }
        return replaced;
    }

    private boolean isPlainExpression(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith(EasyLogConsts.POUND_KEY) || trimmed.startsWith("@") || trimmed.startsWith("T(");
    }

    private boolean isSingleSpelBlock(String value) {
        if (value == null) {
            return false;
        }
        Matcher matcher = SPEL_BLOCK.matcher(value);
        if (!matcher.find()) {
            return false;
        }
        if (matcher.start() != 0 || matcher.end() != value.length()) {
            return false;
        }
        return !matcher.find();
    }

    private Object safeEval(String expr, AnnotatedElementKey key, EvaluationContext ctx) {
        if (expr == null) {
            return null;
        }
        try {
            return cachedExpressionEvaluator.parseExpression(expr, key, ctx);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "SpEL 解析失败: " + expr, e);
            return null;
        }
    }

    private String stringVal(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof CharSequence) {
            return v.toString();
        }
        try {
            return JSON.toJSONString(v);
        } catch (Exception ignore) {
            return String.valueOf(v);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
