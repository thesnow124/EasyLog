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
 * 核心模板解析器，支持两类占位：
 * <ul>
 *     <li>自定义函数块：<code>{funcName{ SpEL }}</code>，可配置前置/后置执行。</li>
 *     <li>纯 SpEL 块：<code>{{ SpEL }}</code>，可引用参数、返回值、异常信息或 Spring Bean。</li>
 * </ul>
 * 解析流程：前置阶段缓存需要提前执行的函数结果；后置阶段先替换函数块，再替换 SpEL，最后回退解析纯表达式。
 * @author gaoshuanglong
 */
public class EasyLogParser implements BeanFactoryAware {

    private static final Logger LOG = Logger.getLogger(EasyLogParser.class.getName());

    private BeanFactory beanFactory;
    private final ParseFunctionFactory parseFunctionFactory;

    private static final Pattern FUNC_BLOCK = Pattern.compile("\\{\\s*([\\w.-]+)\\s*\\{(.*?)\\}\\s*\\}");
    private static final Pattern SPEL_BLOCK = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");

    private final EasyLogCachedExpressionEvaluator cachedExpressionEvaluator = new EasyLogCachedExpressionEvaluator();

    public EasyLogParser(ParseFunctionFactory parseFunctionFactory) {
        this.parseFunctionFactory = parseFunctionFactory;
    }

    /**
     * After method execution: render all templates. Functions pre-evaluated in before-phase are reused.
     */
    public Map<String, String> processAfterExec(List<String> expressTemplate,
                                                Map<String, String> beforeCache,
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
    public Map<String, String> processAfterExec(List<String> expressTemplate,
                                                Map<String, String> beforeCache,
                                                Method method,
                                                Object[] args,
                                                Class<?> targetClass,
                                                String errMsg,
                                                Object result,
                                                Map<String, Object> localVars) {
        HashMap<String, String> map = new HashMap<>();
        if (CollectionUtils.isEmpty(expressTemplate)) {
            return map;
        }
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, targetClass);
        EvaluationContext ctx = cachedExpressionEvaluator.createEvaluationContext(method, args, beanFactory, errMsg, result, localVars);
        for (String template : expressTemplate) {
            String resolved = resolveTemplate(template, elementKey, ctx, beforeCache);
            map.put(template, resolved);
        }
        return map;
    }

    /**
     * Before method execution: pre-evaluate functions that声明 executeBefore=true.
     * 仅会缓存标记为前置执行的自定义函数，避免业务执行后再取“旧值”。
     */
    public Map<String, String> processBeforeExec(List<String> templates,
                                                 Method method,
                                                 Object[] args,
                                                 Class<?> targetClass) {
        HashMap<String, String> cache = new HashMap<>();
        if (CollectionUtils.isEmpty(templates)) {
            return cache;
        }
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, targetClass);
        EvaluationContext ctx = cachedExpressionEvaluator.createEvaluationContext(method, args, beanFactory, null, null);
        for (String template : templates) {
            Matcher m = FUNC_BLOCK.matcher(template);
            while (m.find()) {
                String placeholder = m.group(0);
                String funcName = m.group(1);
                // 仅处理标记为“前置执行”的自定义函数，典型用于查询旧值
                if (!parseFunctionFactory.isBeforeFunction(funcName)) {
                    continue;
                }
                String expr = m.group(2);
                // 先解析函数参数中的 SpEL，再把结果传给自定义函数
                String spelVal = stringVal(safeEval(expr, elementKey, ctx));
                String fnVal = applyFunction(funcName, spelVal);
                // 使用整个占位符作为 key，后置阶段遇到同样占位符时直接复用
                cache.put(placeholder, fnVal);
            }
        }
        return cache;
    }

    private String resolveTemplate(String template,
                                   AnnotatedElementKey elementKey,
                                   EvaluationContext ctx,
                                   Map<String, String> beforeCache) {
        // step1: replace function blocks（处理 {funcName{...}} 占位，优先用前置缓存）
        Matcher funcMatcher = FUNC_BLOCK.matcher(template);
        StringBuffer funcBuf = new StringBuffer();
        boolean funcMatched = false;
        while (funcMatcher.find()) {
            funcMatched = true;
            String placeholder = funcMatcher.group(0);
            String funcName = funcMatcher.group(1);
            String expr = funcMatcher.group(2);
            // 如果前置阶段已缓存该占位结果，直接复用；否则现算
            String replacement = beforeCache != null && beforeCache.containsKey(placeholder)
                    ? beforeCache.get(placeholder)
                    : applyFunction(funcName, stringVal(safeEval(expr, elementKey, ctx)));
            funcMatcher.appendReplacement(funcBuf, Matcher.quoteReplacement(replacement));
        }
        funcMatcher.appendTail(funcBuf);
        String afterFunction = funcBuf.toString();

        // step2: replace SpEL blocks {{ ... }}（处理纯 SpEL 占位）
        Matcher spelMatcher = SPEL_BLOCK.matcher(afterFunction);
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

        // step3: if no placeholder matched but it is a plain expression, evaluate whole
        if (!funcMatched && !spelMatched && isPlainExpression(replaced)) {
            Object v = safeEval(replaced, elementKey, ctx);
            return v == null ? "" : String.valueOf(v);
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

    private String applyFunction(String funcName, String arg) {
        ParseFunction function = parseFunctionFactory.getFunction(funcName);
        if (function == null) {
            LOG.warning("未找到自定义函数: " + funcName);
            return arg == null ? "" : arg;
        }
        try {
            String val = function.apply(arg);
            return val == null ? "" : val;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "自定义函数执行异常: " + funcName, e);
            return "";
        }
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
