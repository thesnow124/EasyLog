package com.github.easylog.util;

import com.github.easylog.compare.FieldInfo;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.service.OpLogContext;

import java.util.Collections;
import java.util.List;

/**
 * @author Gaosl
 * @project EasyLog
 * @description 手动记录日志
 * @date 2024/5/27 14:27:39
 */
public class EasyLogUtil {

    public static void record(String bizNo, String module, String type, String content, String[] contentParam) {
        EasyLogInfo easyLogInfo = new EasyLogInfo();
        easyLogInfo.setBizNo(bizNo);
        easyLogInfo.setModule(module);
        easyLogInfo.setType(type);
        easyLogInfo.setContent(content);
        easyLogInfo.setContentParam(contentParam);
        record(easyLogInfo);
    }

    public static void record(List<EasyLogInfo> list) {
        OpLogContext.peekAndAddLogStack(list);
    }

    public static void record(EasyLogInfo o) {
        record(Collections.singletonList(o));
    }

    public static void record(String bizNo, String module, String type, String content, String[] contentParam, List<FieldInfo> fieldInfoList) {
        EasyLogInfo easyLogInfo = new EasyLogInfo();
        easyLogInfo.setBizNo(bizNo);
        easyLogInfo.setModule(module);
        easyLogInfo.setType(type);
        easyLogInfo.setContent(content);
        easyLogInfo.setContentParam(contentParam);
        easyLogInfo.setFieldInfoList(fieldInfoList);
        record(easyLogInfo);
    }



    public static void record(String bizNo, String module, String type, List<FieldInfo> fieldInfoList) {
        EasyLogInfo easyLogInfo = new EasyLogInfo();
        easyLogInfo.setBizNo(bizNo);
        easyLogInfo.setModule(module);
        easyLogInfo.setType(type);
        easyLogInfo.setFieldInfoList(fieldInfoList);
        record(easyLogInfo);
    }


}
