package com.github.easylog.context;

import com.github.easylog.function.IFunctionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypedValue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SpEL 方法解析器：将根对象上的函数调用路由到 {@link IFunctionService}。
 */
public class EasyLogFunctionMethodResolver implements MethodResolver {

    private static final Logger LOG = Logger.getLogger(EasyLogFunctionMethodResolver.class.getName());

    private final IFunctionService functionService;

    public EasyLogFunctionMethodResolver(IFunctionService functionService) {
        this.functionService = functionService;
    }

    @Override
    public MethodExecutor resolve(EvaluationContext context,
                                  Object targetObject,
                                  String name,
                                  List<TypeDescriptor> argumentTypes) {
        if (targetObject != null && !(targetObject instanceof EasyLogSpelRoot)) {
            return null;
        }
        if (targetObject != null && hasMethodName(targetObject.getClass(), name)) {
            // 保留内置方法优先级
            return null;
        }
        return new FunctionMethodExecutor(name, functionService);
    }

    private boolean hasMethodName(Class<?> type, String methodName) {
        if (type == null || methodName == null) {
            return false;
        }
        Method[] methods = type.getMethods();
        for (Method method : methods) {
            if (methodName.equals(method.getName())) {
                return true;
            }
        }
        return false;
    }

    private static final class FunctionMethodExecutor implements MethodExecutor {
        private final String functionName;
        private final IFunctionService functionService;

        private FunctionMethodExecutor(String functionName, IFunctionService functionService) {
            this.functionName = functionName;
            this.functionService = functionService;
        }

        @Override
        public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
            if (functionService == null) {
                return TypedValue.NULL;
            }
            try {
                Object value = functionService.apply(functionName, arguments);
                return new TypedValue(value);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "函数执行异常: " + functionName, e);
                return TypedValue.NULL;
            }
        }
    }
}
