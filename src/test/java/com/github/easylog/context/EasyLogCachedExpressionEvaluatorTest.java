package com.github.easylog.context;

import com.github.easylog.configuration.EasyLogProperties;
import com.github.easylog.diff.DefaultDiffEngine;
import com.github.easylog.diff.DiffDTO;
import com.github.easylog.function.DefaultFunctionServiceImpl;
import com.github.easylog.function.IParseFunction;
import com.github.easylog.function.ParseFunctionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyLogCachedExpressionEvaluatorTest {

    @AfterEach
    void tearDown() {
        EasyLogContext.clearAll();
    }

    static class Sample {
        String join(String name, int count) {
            return name + count;
        }
    }

    static class EchoBean {
        public String echo(String value) {
            return "bean:" + value;
        }
    }

    @Test
    void evaluationContextExposesArgsVariablesAndResult() throws Exception {
        Method method = Sample.class.getDeclaredMethod("join", String.class, int.class);
        EasyLogContext.push();
        EasyLogContext.put("extra", "x");

        EasyLogCachedExpressionEvaluator evaluator = new EasyLogCachedExpressionEvaluator(null);
        EvaluationContext ctx = evaluator.createEvaluationContext(method, new Object[]{"hi", 2}, null, "err", "res");
        AnnotatedElementKey key = new AnnotatedElementKey(method, Sample.class);

        assertEquals("hi", evaluator.parseExpression("#name", key, ctx));
        assertEquals(2, evaluator.parseExpression("#count", key, ctx));
        assertEquals("x", evaluator.parseExpression("#extra", key, ctx));
        assertEquals("res", evaluator.parseExpression("#_result", key, ctx));
        assertEquals("err", evaluator.parseExpression("#_errMsg", key, ctx));
    }

    @Test
    void evaluationContextResolvesBeanMethods() throws Exception {
        Method method = Sample.class.getDeclaredMethod("join", String.class, int.class);
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("echoBean", new EchoBean());

        EasyLogCachedExpressionEvaluator evaluator = new EasyLogCachedExpressionEvaluator(null);
        EvaluationContext ctx = evaluator.createEvaluationContext(method, new Object[]{"hi", 2}, beanFactory, null, null);
        AnnotatedElementKey key = new AnnotatedElementKey(method, Sample.class);

        assertEquals("bean:hi", evaluator.parseExpression("@echoBean.echo(#name)", key, ctx));
    }

    @Test
    void evaluation_context_resolves_registered_parse_function() throws Exception {
        Method method = Sample.class.getDeclaredMethod("join", String.class, int.class);
        IParseFunction diff = new DefaultDiffEngine(new EasyLogProperties());
        EasyLogCachedExpressionEvaluator evaluator = new EasyLogCachedExpressionEvaluator(
                new DefaultFunctionServiceImpl(new ParseFunctionFactory(Collections.singletonList(diff))));
        EvaluationContext ctx = evaluator.createEvaluationContext(method, new Object[]{"hi", 2}, null, null, null);
        AnnotatedElementKey key = new AnnotatedElementKey(method, Sample.class);

        Object result = evaluator.parseExpression("DIFF(#name,#name)", key, ctx);
        assertTrue(result instanceof DiffDTO);
    }
}
