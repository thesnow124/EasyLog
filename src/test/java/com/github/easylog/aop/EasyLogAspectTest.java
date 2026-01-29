package com.github.easylog.aop;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.api.ILogRecordService;
import com.github.easylog.api.IOperatorService;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.function.ParseFunctionFactory;
import com.github.easylog.model.EasyLogInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyLogAspectTest {

    static class Target {
        @EasyLog(
                module = "m",
                type = "t",
                bizNo = "{{#name}}",
                success = "hello {{#name}}"
        )
        public String greet(String name) {
            return "ok-" + name;
        }
    }

    static class FailingTarget {
        @EasyLog(
                module = "m",
                type = "t",
                bizNo = "1",
                success = "ok",
                fail = "fail {{#_errMsg}}"
        )
        public String boom(String name) {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void aroundSuccessRecordsLog() throws Throwable {
        RecordingLogRecordService recordService = new RecordingLogRecordService();
        EasyLogParser parser = new EasyLogParser(new ParseFunctionFactory(Arrays.asList()));
        EasyLogAspect aspect = new EasyLogAspect(recordService, new FixedOperatorService(), parser, false);

        Target target = new Target();
        Method method = Target.class.getDeclaredMethod("greet", String.class);
        ProceedingJoinPoint joinPoint = new SimpleProceedingJoinPoint(
                target, method, new Object[]{"bob"}, new String[]{"name"});

        Object result = aspect.around(joinPoint);
        assertEquals("ok-bob", result);
        assertEquals(1, recordService.logs.size());

        EasyLogInfo info = recordService.logs.get(0);
        assertEquals("bob", info.getBizNo());
        assertEquals("hello bob", info.getContent());
        assertEquals("fixed-op", info.getOperator());
        assertEquals("fixed-plat", info.getPlatform());
        assertTrue(info.getSuccess());
        assertNotNull(info.getOperateTime());
    }

    @Test
    void aroundFailureRecordsAndRethrows() throws Exception {
        RecordingLogRecordService recordService = new RecordingLogRecordService();
        EasyLogParser parser = new EasyLogParser(new ParseFunctionFactory(Arrays.asList()));
        EasyLogAspect aspect = new EasyLogAspect(recordService, new FixedOperatorService(), parser, false);

        FailingTarget target = new FailingTarget();
        Method method = FailingTarget.class.getDeclaredMethod("boom", String.class);
        ProceedingJoinPoint joinPoint = new SimpleProceedingJoinPoint(
                target, method, new Object[]{"bob"}, new String[]{"name"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> aspect.around(joinPoint));
        assertEquals("boom", ex.getMessage());
        assertEquals(1, recordService.logs.size());

        EasyLogInfo info = recordService.logs.get(0);
        assertEquals("fail boom", info.getContent());
        assertFalse(info.getSuccess());
        assertEquals("boom", info.getErrorMsg());
        assertNotNull(info.getStackTrace());
    }

    static class FixedOperatorService implements IOperatorService {
        @Override
        public String getOperator() {
            return "fixed-op";
        }

        @Override
        public String getPlatform() {
            return "fixed-plat";
        }
    }

    static class RecordingLogRecordService implements ILogRecordService {
        private final List<EasyLogInfo> logs = new ArrayList<>();

        @Override
        public void record(EasyLogInfo easyLogInfo) {
            logs.add(easyLogInfo);
        }
    }

    static class SimpleProceedingJoinPoint implements ProceedingJoinPoint {
        private final Object target;
        private final Method method;
        private Object[] args;
        private final MethodSignature signature;

        SimpleProceedingJoinPoint(Object target, Method method, Object[] args, String[] paramNames) {
            this.target = target;
            this.method = method;
            this.args = args;
            this.signature = new SimpleMethodSignature(method, paramNames);
        }

        @Override
        public Object proceed() throws Throwable {
            return invokeTarget(method, target, args);
        }

        @Override
        public Object proceed(Object[] args) throws Throwable {
            this.args = args;
            return invokeTarget(method, target, args);
        }

        @Override
        public void set$AroundClosure(org.aspectj.runtime.internal.AroundClosure aroundClosure) {
        }

        @Override
        public Object getThis() {
            return target;
        }

        @Override
        public Object getTarget() {
            return target;
        }

        @Override
        public Object[] getArgs() {
            return args;
        }

        @Override
        public Signature getSignature() {
            return signature;
        }

        @Override
        public org.aspectj.lang.reflect.SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public String getKind() {
            return "method-execution";
        }

        @Override
        public StaticPart getStaticPart() {
            return null;
        }

        @Override
        public String toShortString() {
            return method.toString();
        }

        @Override
        public String toLongString() {
            return method.toGenericString();
        }
    }

    private static Object invokeTarget(Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    static class SimpleMethodSignature implements MethodSignature {
        private final Method method;
        private final String[] paramNames;

        SimpleMethodSignature(Method method, String[] paramNames) {
            this.method = method;
            this.paramNames = paramNames;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public String[] getParameterNames() {
            return paramNames;
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public Class<?> getDeclaringType() {
            return method.getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return method.getDeclaringClass().getName();
        }

        @Override
        public String toShortString() {
            return method.toString();
        }

        @Override
        public String toLongString() {
            return method.toGenericString();
        }

        @Override
        public String toString() {
            return method.toString();
        }
    }
}
