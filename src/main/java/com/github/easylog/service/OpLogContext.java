package com.github.easylog.service;

import com.github.easylog.model.EasyLogInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Gaosl
 * @project EasyLog
 * @description 日志ThreadLocal
 * @date 2024/5/15 11:00:01
 */
@Slf4j
@NoArgsConstructor
//todo 待完善
public class OpLogContext {

    private static ThreadLocal<List<EasyLogInfo>> logInfoList = new ThreadLocal<>();


    public static void initOpInfo() {
        List<EasyLogInfo> easyLogInfos = logInfoList.get();
        if (Objects.isNull(easyLogInfos)) {
            logInfoList.set(new ArrayList<>());
        }
    }

    public static List<EasyLogInfo> getOpInfo() {
        return logInfoList.get();
    }

    public static void addAllLogInfoList(List<EasyLogInfo> list) {
        logInfoList.set(list);
    }


}
