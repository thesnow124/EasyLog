package com.github.easylog.function.impl;


import com.github.easylog.function.ICustomFunction;


public class DefaultCustomFunction implements ICustomFunction {
    @Override
    public boolean executeBefore() {
        return false;
    }

    @Override
    public String functionName() {
        return "defaultName";
    }

    @Override
    public String apply(Object value) {
        return null;
    }
}
