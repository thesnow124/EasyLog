package com.github.easylog.service;


import com.github.easylog.model.EasyLogInfo;

/**
 * @author Gaosl
 */
public interface ILogRecordService {
    /**
     * 保存 log
     *
     * @param easyLogInfo 日志实体
     */
    void record(EasyLogInfo easyLogInfo);

}