package com.github.easylog.service.impl;


import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Gaosl
 */
@Slf4j
public class DefaultLogRecordServiceImpl implements ILogRecordService {


    @Override
    public void record(EasyLogInfo easyLogInfo) {
        log.info("【logRecord】log={}", JsonUtils.toJSONString(easyLogInfo));
    }
}