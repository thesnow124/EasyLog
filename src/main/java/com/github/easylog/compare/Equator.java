package com.github.easylog.compare;

import com.alibaba.fastjson2.JSON;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.diff.changetype.ReferenceChange;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ContainerElementChange;
import org.javers.core.diff.changetype.container.ElementValueChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.diff.changetype.container.ValueRemoved;
import org.javers.core.diff.changetype.map.EntryAdded;
import org.javers.core.diff.changetype.map.EntryChange;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 使用 JaVers 对 JSON 对象进行差异对比，输出字段变更列表。
 * <p>
 * 特性：
 * <ul>
 *     <li>忽略 List 顺序（SIMPLE 算法），便于对比表单类 JSON。</li>
 *     <li>保持原始值（字符串化）用于展示与存储。</li>
 *     <li>非 JSON 输入时退化为简单的旧/新值记录。</li>
 * </ul>
 * @author gaoshuanglong
 */
public class Equator {

    private static final Javers JAVERS_IGNORE_LIST_ORDER = JaversBuilder.javers()
            .withListCompareAlgorithm(ListCompareAlgorithm.SIMPLE)
            .build();

    public static List<FieldInfo> getDiffField(Object oldBean, Object newBean) {
        // 1) 空输入直接返回空
        if (oldBean == null && newBean == null) {
            return Collections.emptyList();
        }
        Object left = normalize(oldBean);
        Object right = normalize(newBean);

        // 2) 标量值或类型不兼容：回退为旧/新值
        if (!isStructured(left) || !isStructured(right)) {
            return fallbackScalar(oldBean, newBean);
        }
        try {
            Diff diff = JAVERS_IGNORE_LIST_ORDER.compare(left, right);
            if (!diff.hasChanges()) {
                return new ArrayList<>();
            }

            List<FieldInfo> list = new ArrayList<>();
            for (Change change : diff.getChanges()) {
                if (change instanceof ValueChange) {
                    // 简单属性值变更
                    ValueChange vc = (ValueChange) change;
                    FieldInfo f = new FieldInfo();
                    f.setFieldName(vc.getPropertyNameWithPath());
                    f.setOldFieldVal(stringValue(vc.getLeft()));
                    f.setNewFieldVal(stringValue(vc.getRight()));
                    list.add(f);
                } else if (change instanceof ReferenceChange) {
                    // 引用对象变更（取左右对象，若不存在用 GlobalId）
                    ReferenceChange rc = (ReferenceChange) change;
                    FieldInfo f = new FieldInfo();
                    f.setFieldName(rc.getPropertyNameWithPath());
                    f.setOldFieldVal(stringValue(rc.getLeftObject().orElse(rc.getLeft())));
                    f.setNewFieldVal(stringValue(rc.getRightObject().orElse(rc.getRight())));
                    list.add(f);
                } else if (change instanceof MapChange) {
                    // Map 键值变化：新增/删除/值变更
                    MapChange<?> mc = (MapChange<?>) change;
                    String base = mc.getPropertyNameWithPath();
                    for (EntryChange ec : mc.getEntryChanges()) {
                        FieldInfo f = new FieldInfo();
                        f.setFieldName(base + "[" + stringValue(ec.getKey()) + "]");
                        if (ec instanceof EntryAdded) {
                            f.setOldFieldVal("");
                            f.setNewFieldVal(stringValue(((EntryAdded) ec).getValue()));
                        } else if (ec instanceof EntryRemoved) {
                            f.setOldFieldVal(stringValue(((EntryRemoved) ec).getValue()));
                            f.setNewFieldVal("");
                        } else if (ec instanceof EntryValueChange) {
                            EntryValueChange evc = (EntryValueChange) ec;
                            f.setOldFieldVal(stringValue(evc.getLeftValue()));
                            f.setNewFieldVal(stringValue(evc.getRightValue()));
                        } else {
                            // 未知场景兜底
                            f.setOldFieldVal(stringValue(mc.getLeft()));
                            f.setNewFieldVal(stringValue(mc.getRight()));
                        }
                        list.add(f);
                    }
                } else if (change instanceof ContainerChange) {
                    // List/Set 等集合：新增、删除、元素值变更
                    ContainerChange<?> cc = (ContainerChange<?>) change;
                    String base = cc.getPropertyNameWithPath();
                    for (ValueAdded add : cc.getValueAddedChanges()) {
                        FieldInfo f = new FieldInfo();
                        f.setFieldName(appendIndex(base, add.getIndex(), "+"));
                        f.setOldFieldVal("");
                        f.setNewFieldVal(stringValue(add.getAddedValue()));
                        list.add(f);
                    }
                    for (ValueRemoved rem : cc.getValueRemovedChanges()) {
                        FieldInfo f = new FieldInfo();
                        f.setFieldName(appendIndex(base, rem.getIndex(), "-"));
                        f.setOldFieldVal(stringValue(rem.getRemovedValue()));
                        f.setNewFieldVal("");
                        list.add(f);
                    }
                    for (ContainerElementChange elementChange : cc.getChanges()) {
                        if (elementChange instanceof ElementValueChange) {
                            ElementValueChange evc = (ElementValueChange) elementChange;
                            FieldInfo f = new FieldInfo();
                            f.setFieldName(appendIndex(base, evc.getIndex(), null));
                            f.setOldFieldVal(stringValue(evc.getLeftValue()));
                            f.setNewFieldVal(stringValue(evc.getRightValue()));
                            list.add(f);
                        }
                    }
                } else if (change instanceof PropertyChange) {
                    // 其他属性变更兜底
                    PropertyChange pc = (PropertyChange) change;
                    FieldInfo f = new FieldInfo();
                    f.setFieldName(pc.getPropertyNameWithPath());
                    f.setOldFieldVal(stringValue(pc.getLeft()));
                    f.setNewFieldVal(stringValue(pc.getRight()));
                    list.add(f);
                }
            }
            return list;
        } catch (Exception e) {
            return fallbackScalar(oldBean, newBean);
        }
    }

    private static String appendIndex(String base, Integer idx, String suffixFlag) {
        String suffix = idx == null ? (suffixFlag == null ? "[*]" : "[" + suffixFlag + "]") : "[" + idx + "]";
        return base + suffix;
    }

    private static Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence) {
            String s = value.toString().trim();
            try {
                if (s.startsWith("[")) {
                    return JSON.parseObject(s, List.class);
                }
                if (s.startsWith("{")) {
                    return JSON.parseObject(s, Map.class);
                }
            } catch (Exception e) {
                // ignore and fall back to raw string
            }
            return value.toString();
        }
        return value;
    }

    private static boolean isStructured(Object value) {
        return value != null && !isScalar(value);
    }

    private static boolean isScalar(Object value) {
        return value == null
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum
                || value instanceof java.util.Date
                || value instanceof java.time.temporal.Temporal;
    }

    private static List<FieldInfo> fallbackScalar(Object oldBean, Object newBean) {
        FieldInfo fieldDiff = new FieldInfo();
        fieldDiff.setOldFieldVal(stringValue(oldBean));
        fieldDiff.setNewFieldVal(stringValue(newBean));
        return Collections.singletonList(fieldDiff);
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
