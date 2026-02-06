package com.github.easylog.function;


import com.alibaba.fastjson2.JSON;
import com.github.easylog.constants.EasyLogConsts;
import com.github.easylog.context.EasyLogCachedExpressionEvaluator;
import com.github.easylog.diff.DiffEngine;
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
 * 核心模板解析器，支持 SpEL 块与纯表达式回退。
 * <ul>
 *     <li>SpEL 块：<code>{{ SpEL }}</code>，可引用参数、返回值、异常信息或 Spring Bean。</li>
 * </ul>
 * 解析流程：后置阶段先替换 SpEL，再回退解析纯表达式。
 * @author gaoshuanglong
 */
public class EasyLogParser implements BeanFactoryAware {

    private static final Logger LOG = Logger.getLogger(EasyLogParser.class.getName());

    private BeanFactory beanFactory;

    private static final Pattern SPEL_BLOCK = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");

    private final EasyLogCachedExpressionEvaluator cachedExpressionEvaluator;

    public EasyLogParser(DiffEngine diffEngine) {
        this.cachedExpressionEvaluator = new EasyLogCachedExpressionEvaluator(diffEngine);
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
            Object resolved = resolveTemplate(template, elementKey, ctx, beforeCache);
            map.put(template, resolved);
        }
        return map;
    }

    /**
     * Before method execution: no-op (reserved for future extensions).
     */
    public Map<String, Object> processBeforeExec(List<String> templates,
                                                 Method method,
                                                 Object[] args,
                                                 Class<?> targetClass) {
        return new HashMap<>();
    }

    private Object resolveTemplate(String template,
                                   AnnotatedElementKey elementKey,
                                   EvaluationContext ctx,
                                   Map<String, Object> beforeCache) {
        if (template == null) {
            return null;
        }
        String trimmed = template.trim();
        if (isSingleSpelBlock(trimmed)) {
            Matcher matcher = SPEL_BLOCK.matcher(trimmed);
            if (matcher.matches()) {
                String expr = matcher.group(1);
                return safeEval(expr, elementKey, ctx);
            }
        }
        // replace SpEL blocks {{ ... }}（处理纯 SpEL 占位）
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

        // if no placeholder matched but it is a plain expression, evaluate whole
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
        try { return JSON.toJSONString(v); } catch (Exception ignore) { return String.valueOf(v); }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
