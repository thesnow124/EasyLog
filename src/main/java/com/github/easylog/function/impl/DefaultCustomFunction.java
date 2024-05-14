package com.github.easylog.function.impl;


import com.github.easylog.function.ICustomFunction;


/**
 * @author Gaosl
 */
public class DefaultCustomFunction implements ICustomFunction {

    @Override
    public String functionName() {
        return "defaultName";
    }


    @Override
    public String apply(Object value) {
        return null;
    }
}
