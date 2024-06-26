package com.github.easylog.service.impl;


import com.alibaba.fastjson.JSON;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.util.PlaceholderResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Gaosl
 */
@Slf4j
public class DefaultLogRecordServiceImpl implements ILogRecordService {


    @Override
    public void record(EasyLogInfo easyLogInfo) {
        String resolve = PlaceholderResolver.getDefaultResolver().resolve(easyLogInfo.getContent(), easyLogInfo.getContentParam());
        easyLogInfo.setContent(resolve);
        log.info("【logRecord】log={}", JSON.toJSONString(easyLogInfo));
    }

}