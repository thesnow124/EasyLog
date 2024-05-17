package com.github.easylog.compare;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Gaosl
 * @project EasyLog
 * @description 默认比较器实现
 * @date 2024/5/10 09:46:34
 */
@Slf4j
public class Equator {

    public static List<FieldInfo> getDiffField(Object oldBean, Object newBean) {
        if (Objects.isNull(oldBean) && Objects.isNull(newBean)) {
            return new ArrayList<>();
        }
        HashMap<String, String> oldMap = getMap(oldBean);
        HashMap<String, String> newMap = getMap(newBean);
        return Arrays.asList(oldMap.keySet(), newMap.keySet()).stream().flatMap(Collection::stream).map(k -> {
            FieldInfo fieldDiff = new FieldInfo();
            fieldDiff.setFieldName(k);
            fieldDiff.setNewFieldVal(newMap.get(k));
            fieldDiff.setOldFieldVal(oldMap.get(k));
            return fieldDiff;
        }).collect(Collectors.toList());
    }

    private static HashMap<String, String> getMap(Object bean) {
        if (Objects.isNull(bean)) {
            return new HashMap<>();
        }
        Class<?> clazz = bean.getClass();
        Field[] fieldList = clazz.getFields();
        HashMap<String, String> map = (HashMap<String, String>) Arrays.stream(fieldList)
                .peek(f -> f.setAccessible(true))
                .collect(Collectors.toMap(Field::getName, f -> {
                    try {
                        return JSON.toJSONString(f.get(bean));
                    } catch (IllegalAccessException e) {
                        log.error("", e);
                    }
                    return null;
                }, (v1, v2) -> v1));
        return map;

    }


}
