package com.github.easylog.support;


import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.util.PlaceholderResolver;

/**
 * @author Gaosl
 */
public class DefaultLogRecordServiceImpl implements ILogRecordService {



    @Override
    public void record(EasyLogInfo easyLogInfo) {
        // 渲染 ${} 顺序占位符后输出日志；保留 JSON 结构便于日志采集
        String resolve = PlaceholderResolver.getDefaultResolver().resolve(easyLogInfo.getContent(), easyLogInfo.getContentParam());
        easyLogInfo.setContent(resolve);
    }

}
