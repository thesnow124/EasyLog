package com.github.easylog.context;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 缓存版 SpEL 解析器，复用 Spring {@link CachedExpressionEvaluator} 的 key 缓存能力，
 * 避免重复编译表达式；同时在创建 EvaluationContext 时注入 BeanFactoryResolver 以支持 @bean 调用。
 * @author Gaosl
 */
public class EasyLogCachedExpressionEvaluator extends CachedExpressionEvaluator {

    private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<>(64);

    public EasyLogCachedExpressionEvaluator() {
    }

    public EvaluationContext createEvaluationContext(Method method, Object[] args, BeanFactory beanFactory, String errMsg, Object result) {
        return createEvaluationContext(method, args, beanFactory, errMsg, result, null);
    }

    public EvaluationContext createEvaluationContext(Method method,
                                                     Object[] args,
                                                     BeanFactory beanFactory,
                                                     String errMsg,
                                                     Object result,
                                                     Map<String, Object> localVars) {
        EasyLogEvaluationContext evaluationContext = new EasyLogEvaluationContext(
                method, args, this.getParameterNameDiscoverer(), localVars);
        evaluationContext.putResult(errMsg, result);
        if (beanFactory != null) {
            // setBeanResolver 主要用于支持SpEL模板中调用指定类的方法，如：@XXService.x(#root)
            evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
        }

        return evaluationContext;
    }

    public Object parseExpression(String expression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return this.getExpression(this.keyCache, methodKey, expression).getValue(evalContext);
    }
}
