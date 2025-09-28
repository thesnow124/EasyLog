package com.github.easylog.function;


import com.alibaba.fastjson2.JSON;
import com.github.easylog.constants.EasyLogConsts;
import com.github.easylog.context.EasyLogCachedExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class EasyLogParser implements BeanFactoryAware {

    private BeanFactory beanFactory;

    // Match {{ SpEL }} blocks
    private static final Pattern SPEL_BLOCK = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");

    private final EasyLogCachedExpressionEvaluator cachedExpressionEvaluator = new EasyLogCachedExpressionEvaluator();

    /**
     * After method execution: render all templates. Expressions pre-evaluated in before-phase are reused.
     */
    public Map<String, String> processAfterExec(List<String> expressTemplate,
                                                Map<String, String> beforeCache,
                                                Method method,
                                                Object[] args,
                                                Class<?> targetClass,
                                                String errMsg,
                                                Object result) {
        HashMap<String, String> map = new HashMap<>();
        if (CollectionUtils.isEmpty(expressTemplate)) {
            return map;
        }
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, targetClass);
        EvaluationContext ctx = cachedExpressionEvaluator.createEvaluationContext(method, args, beanFactory, errMsg, result);
        for (String template : expressTemplate) {
            String resolved = resolveTemplate(template, elementKey, ctx, beforeCache, true);
            map.put(template, resolved);
        }
        return map;
    }

    /**
     * Before method execution: pre-evaluate expressions that don't depend on result/err.
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
            Matcher m = SPEL_BLOCK.matcher(template);
            while (m.find()) {
                String expr = m.group(1);
                if (dependsOnResult(expr) || isAfterMarker(expr)) {
                    continue;
                }
                Object val = safeEval(expr, elementKey, ctx);
                cache.put(expr, val == null ? "" : String.valueOf(val));
            }
            // plain expressions without {{}} are not pre-evaluated here
        }
        return cache;
    }

    private boolean dependsOnResult(String expr) {
        return expr != null && (expr.contains(EasyLogConsts.POUND_KEY + EasyLogConsts.ERR_MSG)
                || expr.contains(EasyLogConsts.POUND_KEY + EasyLogConsts.RESULT));
    }

    private String resolveTemplate(String template,
                                   AnnotatedElementKey elementKey,
                                   EvaluationContext ctx,
                                   Map<String, String> beforeCache,
                                   boolean allowWholeExpr) {
        Matcher m = SPEL_BLOCK.matcher(template);
        StringBuffer sb = new StringBuffer();
        boolean any = false;
        while (m.find()) {
            any = true;
            String expr = m.group(1);
            String replacement = beforeCache != null && beforeCache.containsKey(expr)
                    ? beforeCache.get(expr)
                    : stringVal(safeEval(expr, elementKey, ctx));
            // escape replacement for matcher
            replacement = Matcher.quoteReplacement(replacement);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        if (any) {
            return sb.toString();
        }
        // if template is a plain SpEL (without {{}}), evaluate as whole
        if (allowWholeExpr) {
            Object v = safeEval(template, elementKey, ctx);
            return v == null ? "" : String.valueOf(v);
        }
        return template;
    }

    private Object safeEval(String expr, AnnotatedElementKey key, EvaluationContext ctx) {
        try {
            return cachedExpressionEvaluator.parseExpression(expr, key, ctx);
        } catch (Exception e) {
            log.warn("SpEL 解析失败: {}", expr);
            return null;
        }
    }

    private boolean isAfterMarker(String expr) {
        // If expression intentionally wrapped with @easyLogPhase.after(...),
        // defer to after-phase regardless of result dependency.
        return expr != null && expr.replaceAll("\\s+", "").startsWith("@easyLogPhase.after(");
    }

    private String stringVal(Object v) {
        if (v == null) return "";
        if (v instanceof CharSequence) return v.toString();
        // 对于非字符串结果，默认序列化为 JSON 文本，便于在 detail 的 [old,new] 中直接形成合法 JSON
        try { return JSON.toJSONString(v); } catch (Exception ignore) { return String.valueOf(v); }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
