package com.github.easylog.context;

import com.github.easylog.constants.EasyLogConsts;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;


/**
 * @author Gaosl
 */
public class EasyLogEvaluationContext extends MethodBasedEvaluationContext {

    public EasyLogEvaluationContext(Method method, Object[] arguments, ParameterNameDiscoverer parameterNameDiscoverer) {
        super(null, method, arguments, parameterNameDiscoverer);
//        super.lazyLoadArguments();
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
}
