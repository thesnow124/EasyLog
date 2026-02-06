package com.github.easylog.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local variable context for templates (SpEL) to consume.
 * Typical usage in business code within an annotated method:
 *   EasyLogContext.put("oldAddress", value);
 * <p>
 * 内部使用栈结构以支持嵌套的注解方法调用，避免变量互相覆盖/污染。push/pop 由切面控制，业务只需 put/get。
 * Note: ThreadLocal values do not propagate across async/thread-pool boundaries; pass values explicitly if needed.
 * @author gaoshuanglong
 */
public final class EasyLogContext {

    private static final ThreadLocal<Deque<Map<String, Object>>> VAR_STACK = new ThreadLocal<>();

    private EasyLogContext() {}

    public static void push() {
        Deque<Map<String, Object>> deque = VAR_STACK.get();
        if (deque == null) {
            deque = new ArrayDeque<>();
            VAR_STACK.set(deque);
        }
        deque.push(new HashMap<>());
    }

    public static Map<String, Object> pop() {
        Deque<Map<String, Object>> deque = VAR_STACK.get();
        if (deque == null || deque.isEmpty()) {
            return null;
        }
        return deque.pop();
    }

    public static void clearIfEmpty() {
        Deque<Map<String, Object>> deque = VAR_STACK.get();
        if (deque == null || deque.isEmpty()) {
            VAR_STACK.remove();
        }
    }

    public static void clearAll() {
        VAR_STACK.remove();
    }

    public static void put(String key, Object value) {
        Deque<Map<String, Object>> deque = VAR_STACK.get();
        if (deque == null || deque.isEmpty()) {
            // ignore silently to avoid throwing in business; can be validated by aspect if needed
            return;
        }
        Map<String, Object> map = deque.peek();
        if (map != null) {
            map.put(key, value);
        }
    }

    public static Object get(String key) {
        Deque<Map<String, Object>> deque = VAR_STACK.get();
        if (deque == null || deque.isEmpty()) {
            return null;
        }
        Map<String, Object> map = deque.peek();
        return map == null ? null : map.get(key);
    }

    /**
     * Returns the variables available from the top-most context, or empty map if none.
     */
    public static Map<String, Object> getVariables() {
        Deque<Map<String, Object>> deque = VAR_STACK.get();
        if (deque == null || deque.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> map = deque.peek();
        return map == null ? new HashMap<>() : map;
    }
}
