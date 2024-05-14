package com.github.easylog.service.impl;


import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.service.MultilingualService;
import com.github.easylog.util.JsonUtils;
import com.github.easylog.util.PlaceholderResolver;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.HashMap;

/**
 * @author Gaosl
 */
@Slf4j
public class DefaultLogRecordServiceImpl implements ILogRecordService {

    @Resource
     private MultilingualService multilingualService;

    @Override
    public void record(EasyLogInfo easyLogInfo) {
        HashMap<String, String> multilingualMap = multilingualService.getMultilingualMap("");
        String resolve = PlaceholderResolver.getDefaultResolver().resolve(easyLogInfo.getContent(), easyLogInfo.getParamList());
        easyLogInfo.setContent(resolve);
        log.info("【logRecord】log={}", JsonUtils.toJSONString(easyLogInfo));
    }

}