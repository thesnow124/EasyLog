package com.github.easylog.compare;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 使用 JaVers 对 JSON 对象进行差异对比，输出字段变更列表。
 * <p>
 * 特性：
 * <ul>
 *     <li>忽略 List 顺序（SIMPLE 算法），便于对比表单类 JSON。</li>
 *     <li>自动将日期/科学计数等值转为可读字符串，避免展示 “1970-01-01” 等默认值。</li>
 *     <li>非 JSON 输入时退化为简单的旧/新值记录。</li>
 * </ul>
 * @author gaoshuanglong
 */
@Slf4j
public class Equator {

    private static final Javers JAVERS_IGNORE_LIST_ORDER = JaversBuilder.javers()
            .withListCompareAlgorithm(ListCompareAlgorithm.SIMPLE)
            .build();

    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern PATTERN_WITH_SECONDS = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$");
    private static final DateTimeFormatter FORMATTER_WITH_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Pattern PATTERN_WITHOUT_SECONDS = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$");
    private static final DateTimeFormatter FORMATTER_WITHOUT_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public static List<FieldInfo> getDiffField(String oldBean, String newBean) {
        // 1) 空输入直接返回空
        if (oldBean == null && newBean == null) {
            return Collections.emptyList();
        }
        String oldStr = oldBean == null ? "" : oldBean;
        String newStr = newBean == null ? "" : newBean;
        // 2) 任一非 JSON：不做字段拆分，原样记录旧/新值
        if (!isJsonString(oldStr) || !isJsonString(newStr)) {
            FieldInfo fieldDiff = new FieldInfo();
            fieldDiff.setOldFieldVal(oldStr);
            fieldDiff.setNewFieldVal(newStr);
            return Collections.singletonList(fieldDiff);
        }

        // 3) JSON：转成可比对象后用 JaVers 计算差异
        Object left = toComparable(oldStr);
        Object right = toComparable(newStr);
        Diff diff = JAVERS_IGNORE_LIST_ORDER.compare(left, right);
        if (!diff.hasChanges()) return new ArrayList<>();

        List<FieldInfo> list = new ArrayList<>();
        for (Change change : diff.getChanges()) {
            if (change instanceof ValueChange) {
                // 简单属性值变更
                ValueChange vc = (ValueChange) change;
                FieldInfo f = new FieldInfo();
                f.setFieldName(vc.getPropertyNameWithPath());
                f.setOldFieldVal(formatValue(vc.getLeft()));
                f.setNewFieldVal(formatValue(vc.getRight()));
                list.add(f);
            } else if (change instanceof ReferenceChange) {
                // 引用对象变更（取左右对象，若不存在用 GlobalId）
                ReferenceChange rc = (ReferenceChange) change;
                FieldInfo f = new FieldInfo();
                f.setFieldName(rc.getPropertyNameWithPath());
                f.setOldFieldVal(formatValue(rc.getLeftObject().orElse(rc.getLeft())));
                f.setNewFieldVal(formatValue(rc.getRightObject().orElse(rc.getRight())));
                list.add(f);
            } else if (change instanceof MapChange) {
                // Map 键值变化：新增/删除/值变更
                MapChange<?> mc = (MapChange<?>) change;
                String base = mc.getPropertyNameWithPath();
                for (EntryChange ec : mc.getEntryChanges()) {
                    FieldInfo f = new FieldInfo();
                    f.setFieldName(base + "[" + formatValue(ec.getKey()) + "]");
                    if (ec instanceof EntryAdded) {
                        f.setOldFieldVal("");
                        f.setNewFieldVal(formatValue(((EntryAdded) ec).getValue()));
                    } else if (ec instanceof EntryRemoved) {
                        f.setOldFieldVal(formatValue(((EntryRemoved) ec).getValue()));
                        f.setNewFieldVal("");
                    } else if (ec instanceof EntryValueChange) {
                        EntryValueChange evc = (EntryValueChange) ec;
                        f.setOldFieldVal(formatValue(evc.getLeftValue()));
                        f.setNewFieldVal(formatValue(evc.getRightValue()));
                    } else {
                        // 未知场景兜底
                        f.setOldFieldVal(formatValue(mc.getLeft()));
                        f.setNewFieldVal(formatValue(mc.getRight()));
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
                    f.setNewFieldVal(formatValue(add.getAddedValue()));
                    list.add(f);
                }
                for (ValueRemoved rem : cc.getValueRemovedChanges()) {
                    FieldInfo f = new FieldInfo();
                    f.setFieldName(appendIndex(base, rem.getIndex(), "-"));
                    f.setOldFieldVal(formatValue(rem.getRemovedValue()));
                    f.setNewFieldVal("");
                    list.add(f);
                }
                for (ContainerElementChange elementChange : cc.getChanges()) {
                    if (elementChange instanceof ElementValueChange) {
                        ElementValueChange evc = (ElementValueChange) elementChange;
                        FieldInfo f = new FieldInfo();
                        f.setFieldName(appendIndex(base, evc.getIndex(), null));
                        f.setOldFieldVal(formatValue(evc.getLeftValue()));
                        f.setNewFieldVal(formatValue(evc.getRightValue()));
                        list.add(f);
                    }
                }
            } else if (change instanceof PropertyChange) {
                // 其他属性变更兜底
                PropertyChange pc = (PropertyChange) change;
                FieldInfo f = new FieldInfo();
                f.setFieldName(pc.getPropertyNameWithPath());
                f.setOldFieldVal(formatValue(pc.getLeft()));
                f.setNewFieldVal(formatValue(pc.getRight()));
                list.add(f);
            }
        }
        return list;
    }

    private static String appendIndex(String base, Integer idx, String suffixFlag) {
        String suffix = idx == null ? (suffixFlag == null ? "[*]" : "[" + suffixFlag + "]") : "[" + idx + "]";
        return base + suffix;
    }

    private static boolean isJsonString(String str) {
        boolean result = false;
        try {
            JSON.parse(str);
            result = true;
        } catch (Exception ignored) {
        }
        return result;
    }

    private static Object toComparable(String json) {
        String s = json.trim();
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
        return s;
    }

    private static String formatValue(Object value) {
        if (value == null) return "";
        String str = String.valueOf(value);
        if (StringUtils.isEmpty(str)) return "";
        if ("1970-01-01T00:00:01".equals(str) || "1970-01-01".equals(str)) return "";
        try {
            if (PATTERN_WITH_SECONDS.matcher(str).matches()) {
                LocalDateTime dateTime = LocalDateTime.parse(str, FORMATTER_WITH_SECONDS);
                return dateTime.format(OUTPUT_FORMATTER);
            } else if (PATTERN_WITHOUT_SECONDS.matcher(str).matches()) {
                LocalDateTime dateTime = LocalDateTime.parse(str, FORMATTER_WITHOUT_SECONDS);
                return dateTime.format(OUTPUT_FORMATTER);
            }
        } catch (Exception ignored) {}

        try {
            if (str.contains(".") || str.contains("E") || str.contains("e")) {
                BigDecimal bd = new BigDecimal(str);
                return bd.stripTrailingZeros().toPlainString();
            }
        } catch (NumberFormatException ignored) {}
        return str;
    }
}
