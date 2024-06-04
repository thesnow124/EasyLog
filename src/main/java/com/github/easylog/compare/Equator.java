package com.github.easylog.compare;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import me.codeleep.jsondiff.DefaultJsonDifference;
import me.codeleep.jsondiff.common.model.JsonCompareResult;
import me.codeleep.jsondiff.common.model.TravelPath;
import me.codeleep.jsondiff.core.config.JsonComparedOption;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

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

    public static List<FieldInfo> getDiffField(String oldBean, String newBean) {
        if (Objects.isNull(oldBean)) {
            oldBean = "{}";
        }
        if (Objects.isNull(newBean)) {
            newBean = "{}";
        }
        if (!isJsonString(oldBean) ||! isJsonString(newBean)) {
            FieldInfo fieldDiff = new FieldInfo();
            fieldDiff.setNewFieldVal(oldBean);
            fieldDiff.setNewFieldVal(newBean);
            return Collections.singletonList(fieldDiff);
        }

        JsonComparedOption jsonComparedOption = new JsonComparedOption().setIgnoreOrder(true);
        JsonCompareResult jsonCompareResult = new DefaultJsonDifference()
                .option(jsonComparedOption)
                .detectDiff(newBean, oldBean);
        return jsonCompareResult.getDefectsList().stream()
                .filter(o -> !Objects.equals(o.getActual(), o.getExpect()))
                .map(o -> {
                    FieldInfo fieldDiff = new FieldInfo();
                    String abstractTravelPath = Optional.ofNullable(o.getTravelPath()).map(TravelPath::getAbstractTravelPath).orElse("");
                    List<String> split1 = split(abstractTravelPath, "\\.");
                    if (!CollectionUtils.isEmpty(split1)) {
                        String s = split1.get(split1.size() - 1);
                        fieldDiff.setFieldName(s);
                    }
                    fieldDiff.setOldFieldVal((String) o.getActual());
                    fieldDiff.setNewFieldVal((String) o.getExpect());
                    return fieldDiff;
                }).collect(Collectors.toList());
    }

    public static List<String> split(String listStr, String regex) {
        if (StringUtils.isBlank(listStr)) {
            return new ArrayList<>();
        }
        return Arrays.stream(listStr.split(regex))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    public static boolean isJsonString(String str) {
        boolean result = false;
        try {
            JSON.parse(str);
            result = true;
        } catch (Exception ignored) {
        }
        return result;
    }

}
