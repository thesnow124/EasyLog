package com.github.easylog.diff;

import com.alibaba.fastjson2.JSON;
import com.github.easylog.annotation.EasyLogDiffField;
import com.github.easylog.annotation.EasyLogDiffObject;
import com.github.easylog.configuration.EasyLogProperties;
import com.github.easylog.function.IParseFunction;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认Diff实现：按字段对比（含别名/忽略/空值策略）。
 * @author gaoshuanglong
 */
public class DefaultDiffEngine implements IParseFunction {


    private final boolean diffIgnoreOldObjectNullValue;
    private final boolean diffIgnoreNewObjectNullValue;

    public DefaultDiffEngine(EasyLogProperties properties) {
        this.diffIgnoreOldObjectNullValue = properties.isDiffIgnoreOldObjectNullValue();
        this.diffIgnoreNewObjectNullValue = properties.isDiffIgnoreNewObjectNullValue();
    }

    public DiffDTO diff(Object oldObject, Object newObject) {
        if (oldObject == null || newObject == null) {
            return null;
        }
        String oldClassName = oldObject.getClass().getName();
        String newClassName = newObject.getClass().getName();

        EasyLogDiffObject oldAnno = oldObject.getClass().getDeclaredAnnotation(EasyLogDiffObject.class);
        EasyLogDiffObject newAnno = newObject.getClass().getDeclaredAnnotation(EasyLogDiffObject.class);

        boolean oldClassEnableAllFields = oldAnno != null && oldAnno.enableAllFields();
        boolean newClassEnableAllFields = newAnno != null && newAnno.enableAllFields();

        String oldClassAlias = oldAnno != null && isNotBlank(oldAnno.alias()) ? oldAnno.alias() : null;
        String newClassAlias = newAnno != null && isNotBlank(newAnno.alias()) ? newAnno.alias() : null;

        Map<String, String> oldFieldAliasMap = new LinkedHashMap<>();
        Map<String, String> newFieldAliasMap = new LinkedHashMap<>();
        Map<String, Object> oldValueMap = new LinkedHashMap<>();
        Map<String, Object> newValueMap = new LinkedHashMap<>();

        Field[] oldFields = getAllFields(oldObject.getClass());
        for (Field oldField : oldFields) {
            try {
                oldField.setAccessible(true);
                Object oldValue = oldField.get(oldObject);
                EasyLogDiffField oldFieldAnno = oldField.getDeclaredAnnotation(EasyLogDiffField.class);
                if (!judgeFieldDiffNeeded(oldValue, false, oldClassEnableAllFields, oldFieldAnno)) {
                    continue;
                }
                Field newField = getFieldByName(newObject.getClass(), oldField.getName());
                if (newField == null) {
                    continue;
                }
                EasyLogDiffField newFieldAnno = newField.getDeclaredAnnotation(EasyLogDiffField.class);
                newField.setAccessible(true);
                Object newValue = newField.get(newObject);
                if (!judgeFieldDiffNeeded(newValue, true, newClassEnableAllFields, newFieldAnno)) {
                    continue;
                }
                if (oldFieldAnno != null && newFieldAnno != null) {
                    String oldFieldAlias = isNotBlank(oldFieldAnno.alias()) ? oldFieldAnno.alias() : null;
                    String newFieldAlias = isNotBlank(newFieldAnno.alias()) ? newFieldAnno.alias() : null;
                    oldFieldAliasMap.put(oldField.getName(), oldFieldAlias);
                    newFieldAliasMap.put(newField.getName(), newFieldAlias);
                }
                if (!fieldValueEquals(oldValue, newValue)) {
                    oldValueMap.put(oldField.getName(), oldValue);
                    newValueMap.put(newField.getName(), newValue);
                }
            } catch (Exception ignore) {
                // ignore bad fields
            }
        }

        DiffDTO diffDTO = new DiffDTO();
        diffDTO.setOldClassName(oldClassName);
        diffDTO.setOldClassAlias(oldClassAlias);
        diffDTO.setNewClassName(newClassName);
        diffDTO.setNewClassAlias(newClassAlias);
        List<DiffFieldDTO> diffFieldDTOList = new ArrayList<>();
        diffDTO.setDiffFieldDTOList(diffFieldDTOList);

        for (Map.Entry<String, Object> entry : oldValueMap.entrySet()) {
            String fieldName = entry.getKey();
            Object oldValue = entry.getValue();
            Object newValue = newValueMap.getOrDefault(entry.getKey(), null);
            DiffFieldDTO diffFieldDTO = new DiffFieldDTO();
            diffFieldDTO.setFieldName(fieldName);
            diffFieldDTO.setOldFieldAlias(oldFieldAliasMap.getOrDefault(fieldName, null));
            diffFieldDTO.setNewFieldAlias(newFieldAliasMap.getOrDefault(fieldName, null));
            diffFieldDTO.setOldValue(oldValue);
            diffFieldDTO.setNewValue(newValue);
            diffFieldDTOList.add(diffFieldDTO);
        }
        return diffDTO;
    }

    @Override
    public String functionName() {
        return "DIFF";
    }

    @Override
    public Object apply(Object... values) {
        return diff(values[0], values[1]);
    }

    private boolean judgeFieldDiffNeeded(Object objectValue,
                                         boolean isNewObject,
                                         boolean classEnableAllFields,
                                         EasyLogDiffField fieldAnno) {
        boolean annotationChecker1 = classEnableAllFields && (fieldAnno == null || !fieldAnno.ignored());
        boolean annotationChecker2 = fieldAnno != null && !fieldAnno.ignored();
        boolean ignoreNullValue = objectValue == null &&
                ((isNewObject && diffIgnoreNewObjectNullValue) || (!isNewObject && diffIgnoreOldObjectNullValue));
        return (annotationChecker1 || annotationChecker2) && !ignoreNullValue;
    }

    private static boolean fieldValueEquals(Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) {
            return true;
        }
        if (oldValue == null || newValue == null) {
            return false;
        }
        boolean isAllPrimitive = isWrapClassOrPrimitive(oldValue.getClass()) && isWrapClassOrPrimitive(newValue.getClass());
        boolean isAllNotPrimitive = !isWrapClassOrPrimitive(oldValue.getClass()) && !isWrapClassOrPrimitive(newValue.getClass());
        if (isAllPrimitive) {
            return oldValue.equals(newValue);
        }
        if (isAllNotPrimitive) {
            if (isJsonArray(oldValue) && isJsonArray(newValue)) {
                String oldJson = JSON.toJSONString(oldValue);
                String newJson = JSON.toJSONString(newValue);
                return JSON.parseArray(oldJson).equals(JSON.parseArray(newJson));
            }
            try {
                String oldJson = JSON.toJSONString(oldValue);
                String newJson = JSON.toJSONString(newValue);
                return JSON.parseObject(oldJson).equals(JSON.parseObject(newJson));
            } catch (Exception e) {
                return oldValue.equals(newValue);
            }
        }
        return false;
    }

    private static boolean isWrapClassOrPrimitive(Class<?> clz) {
        return clz.isPrimitive() || clz == Integer.class || clz == Long.class || clz == Short.class
                || clz == Boolean.class || clz == Byte.class || clz == Float.class || clz == Double.class
                || clz == String.class || clz == Character.class;
    }

    private static boolean isJsonArray(Object obj) {
        return obj != null && (obj.getClass().isArray() || obj instanceof Collection);
    }

    private static Field[] getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && !c.isSynthetic(); c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    fields.add(field);
                }
            }
        }
        return fields.toArray(new Field[0]);
    }

    private static Field getFieldByName(Class<?> type, String fieldName) {
        for (Class<?> c = type; c != null && !c.isSynthetic(); c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static boolean isNotBlank(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
