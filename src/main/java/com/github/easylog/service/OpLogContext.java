package com.github.easylog.service;

import com.github.easylog.model.EasyLogInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @author Gaosl
 * @project EasyLog
 * @description 日志ThreadLocal
 * @date 2024/5/15 11:00:01
 */
@Slf4j
@NoArgsConstructor
public class OpLogContext {

    private static ThreadLocal<Stack<List<EasyLogInfo>>> LOG_INFO_STACK = new ThreadLocal<>();


    public static Stack<List<EasyLogInfo>> getLogStack() {
        return LOG_INFO_STACK.get();
    }

    public static void pushLogStack(List<EasyLogInfo> list) {
        Stack<List<EasyLogInfo>> stack = LOG_INFO_STACK.get();
        if (Objects.isNull(stack)) {
            stack= new Stack<>();
        }
        stack.push(list);
        LOG_INFO_STACK.set(stack);
    }

    public static void peekAndAddLogStack(List<EasyLogInfo> list) {
        Stack<List<EasyLogInfo>> stack = LOG_INFO_STACK.get();
        if (Objects.isNull(stack)) {
            return;
        }
        List<EasyLogInfo> peek = stack.peek();
        peek.addAll(list);
    }

    public static List<EasyLogInfo> popLogStack() {
        Stack<List<EasyLogInfo>> stack = LOG_INFO_STACK.get();
        if (Objects.isNull(stack)) {
            return null;
        }
        return stack.pop();
    }

    public static void removeLogStack() {
        LOG_INFO_STACK.remove();
    }


}
