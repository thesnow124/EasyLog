package com.github.easylog.compare;

import com.alibaba.fastjson2.JSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 对对象进行浅层字段差异对比，输出字段变更列表。
 * <p>
 * 特性：
 * <ul>
 *     <li>仅比较第一层字段，不展开嵌套对象。</li>
 *     <li>List/Map 作为整体值比较。</li>
 *     <li>非对象输入时退化为简单的旧/新值记录。</li>
 * </ul>
 * @author gaoshuanglong
 */
public class Equator {

    public static List<FieldInfo> getDiffField(Object oldBean, Object newBean) {
        if (oldBean == null && newBean == null) {
            return Collections.emptyList();
        }
        if (oldBean == null || newBean == null) {
            return Collections.singletonList(buildFieldInfo(null, oldBean, newBean));
        }
        if (isSimpleValue(oldBean) && isSimpleValue(newBean)) {
            return Objects.equals(oldBean, newBean)
                    ? Collections.emptyList()
                    : Collections.singletonList(buildFieldInfo(null, oldBean, newBean));
        }
        if (isMap(oldBean) || isMap(newBean)) {
            if (oldBean instanceof Map && newBean instanceof Map) {
                return diffTopLevelMap((Map<?, ?>) oldBean, (Map<?, ?>) newBean);
            }
            return Collections.singletonList(buildFieldInfo(null, oldBean, newBean));
        }
        if (isListLike(oldBean) || isListLike(newBean)) {
            return containerEquals(oldBean, newBean)
                    ? Collections.emptyList()
                    : Collections.singletonList(buildFieldInfo(null, oldBean, newBean));
        }
        return diffTopLevelFields(oldBean, newBean);
    }

    private static List<FieldInfo> diffTopLevelFields(Object oldBean, Object newBean) {
        List<FieldInfo> list = new ArrayList<>();
        for (java.lang.reflect.Field oldField : getAllFields(oldBean.getClass())) {
            java.lang.reflect.Field newField = getFieldByName(newBean.getClass(), oldField.getName());
            if (newField == null) {
                continue;
            }
            try {
                oldField.setAccessible(true);
                newField.setAccessible(true);
                Object oldValue = oldField.get(oldBean);
                Object newValue = newField.get(newBean);
                if (fieldValueEquals(oldValue, newValue)) {
                    continue;
                }
                list.add(buildFieldInfo(oldField.getName(), oldValue, newValue));
            } catch (Exception ignore) {
                // ignore bad fields to avoid breaking business flow
            }
        }
        return list;
    }

    private static List<FieldInfo> diffTopLevelMap(Map<?, ?> oldMap, Map<?, ?> newMap) {
        List<FieldInfo> list = new ArrayList<>();
        java.util.Set<Object> keys = new java.util.LinkedHashSet<>();
        keys.addAll(oldMap.keySet());
        keys.addAll(newMap.keySet());
        for (Object key : keys) {
            Object oldValue = oldMap.get(key);
            Object newValue = newMap.get(key);
            if (fieldValueEquals(oldValue, newValue)) {
                continue;
            }
            list.add(buildFieldInfo(stringValue(key), oldValue, newValue));
        }
        return list;
    }

    private static FieldInfo buildFieldInfo(String fieldName, Object oldValue, Object newValue) {
        FieldInfo fieldDiff = new FieldInfo();
        fieldDiff.setFieldName(fieldName);
        fieldDiff.setOldFieldVal(stringValue(oldValue));
        fieldDiff.setNewFieldVal(stringValue(newValue));
        return fieldDiff;
    }

    private static boolean fieldValueEquals(Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) {
            return true;
        }
        if (oldValue == null || newValue == null) {
            return false;
        }
        if (isSimpleValue(oldValue) && isSimpleValue(newValue)) {
            return Objects.equals(oldValue, newValue);
        }
        if (isMap(oldValue) || isMap(newValue) || isListLike(oldValue) || isListLike(newValue)) {
            return containerEquals(oldValue, newValue);
        }
        return Objects.equals(stringValue(oldValue), stringValue(newValue));
    }

    private static boolean isSimpleValue(Object value) {
        return value == null
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum
                || value instanceof java.util.Date
                || value instanceof java.time.temporal.Temporal;
    }

    private static boolean isMap(Object value) {
        return value instanceof Map;
    }

    private static boolean isListLike(Object value) {
        return value != null && (value instanceof Iterable || value.getClass().isArray());
    }

    private static boolean containerEquals(Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) {
            return true;
        }
        if (oldValue == null || newValue == null) {
            return false;
        }
        if (oldValue instanceof Map && newValue instanceof Map) {
            return oldValue.equals(newValue);
        }
        if (oldValue instanceof Iterable && newValue instanceof Iterable) {
            return oldValue.equals(newValue);
        }
        if (oldValue.getClass().isArray() && newValue.getClass().isArray()) {
            return arrayEquals(oldValue, newValue);
        }
        return Objects.equals(stringValue(oldValue), stringValue(newValue));
    }

    private static boolean arrayEquals(Object oldValue, Object newValue) {
        if (oldValue instanceof Object[] && newValue instanceof Object[]) {
            return Arrays.deepEquals((Object[]) oldValue, (Object[]) newValue);
        }
        if (oldValue instanceof int[] && newValue instanceof int[]) {
            return Arrays.equals((int[]) oldValue, (int[]) newValue);
        }
        if (oldValue instanceof long[] && newValue instanceof long[]) {
            return Arrays.equals((long[]) oldValue, (long[]) newValue);
        }
        if (oldValue instanceof short[] && newValue instanceof short[]) {
            return Arrays.equals((short[]) oldValue, (short[]) newValue);
        }
        if (oldValue instanceof byte[] && newValue instanceof byte[]) {
            return Arrays.equals((byte[]) oldValue, (byte[]) newValue);
        }
        if (oldValue instanceof char[] && newValue instanceof char[]) {
            return Arrays.equals((char[]) oldValue, (char[]) newValue);
        }
        if (oldValue instanceof boolean[] && newValue instanceof boolean[]) {
            return Arrays.equals((boolean[]) oldValue, (boolean[]) newValue);
        }
        if (oldValue instanceof float[] && newValue instanceof float[]) {
            return Arrays.equals((float[]) oldValue, (float[]) newValue);
        }
        if (oldValue instanceof double[] && newValue instanceof double[]) {
            return Arrays.equals((double[]) oldValue, (double[]) newValue);
        }
        return Objects.equals(stringValue(oldValue), stringValue(newValue));
    }

    private static java.lang.reflect.Field[] getAllFields(Class<?> type) {
        List<java.lang.reflect.Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && !c.isSynthetic(); c = c.getSuperclass()) {
            for (java.lang.reflect.Field field : c.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    fields.add(field);
                }
            }
        }
        return fields.toArray(new java.lang.reflect.Field[0]);
    }

    private static java.lang.reflect.Field getFieldByName(Class<?> type, String fieldName) {
        for (Class<?> c = type; c != null && !c.isSynthetic(); c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        try {
            return JSON.toJSONString(value);
        } catch (Exception ignore) {
            return String.valueOf(value);
        }
    }
}
