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
import org.javers.core.diff.changetype.ValueChange;

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
        if (Objects.isNull(oldBean)) oldBean = "{}";
        if (Objects.isNull(newBean)) newBean = "{}";
        if (!isJsonString(oldBean) || !isJsonString(newBean)) {
            FieldInfo fieldDiff = new FieldInfo();
            fieldDiff.setOldFieldVal(oldBean);
            fieldDiff.setNewFieldVal(newBean);
            return Collections.singletonList(fieldDiff);
        }

        Object left = toComparable(oldBean);
        Object right = toComparable(newBean);
        Diff diff = JAVERS_IGNORE_LIST_ORDER.compare(left, right);
        if (!diff.hasChanges()) return new ArrayList<>();

        List<FieldInfo> list = new ArrayList<>();
        for (Change change : diff.getChanges()) {
            if (change instanceof PropertyChange) {
                PropertyChange pc = (PropertyChange) change;
                if (pc instanceof ValueChange) {
                    ValueChange vc = (ValueChange) pc;
                    FieldInfo f = new FieldInfo();
                    f.setFieldName(pc.getPropertyNameWithPath());
                    f.setOldFieldVal(formatValue(vc.getLeft()));
                    f.setNewFieldVal(formatValue(vc.getRight()));
                    list.add(f);
                }
            }
        }
        return list;
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

