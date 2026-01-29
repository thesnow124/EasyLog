package com.github.easylog.context;

import com.github.easylog.constants.EasyLogConsts;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.Map;


/**
 * SpEL 解析上下文：包含方法参数、结果/错误信息与 EasyLogContext 中的自定义变量。
 * @author gaoshuanglong
 */
public class EasyLogEvaluationContext extends MethodBasedEvaluationContext {

    public EasyLogEvaluationContext(Method method, Object[] arguments, ParameterNameDiscoverer parameterNameDiscoverer) {
        this(method, arguments, parameterNameDiscoverer, null);
    }

    public EasyLogEvaluationContext(Method method,
                                    Object[] arguments,
                                    ParameterNameDiscoverer parameterNameDiscoverer,
                                    Map<String, Object> localVars) {
        super(null, method, arguments, parameterNameDiscoverer);
        Map<String, Object> vars = localVars != null ? localVars : EasyLogContext.getVariables();
        if (vars.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            String key = entry.getKey();
            if (key == null || isReservedVariable(key)) {
                continue;
            }
            super.setVariable(key, entry.getValue());
        }
    }

    /**
     * 将方法执行结果放入上下文中
     *
     * @param errMsg 错误信息
     * @param result 返回结果
     */
    public void putResult(String errMsg, Object result) {
        super.setVariable(EasyLogConsts.ERR_MSG, errMsg);
        super.setVariable(EasyLogConsts.RESULT, result);
    }

    private boolean isReservedVariable(String key) {
        return EasyLogConsts.RESULT.equals(key) || EasyLogConsts.ERR_MSG.equals(key);
    }
}
