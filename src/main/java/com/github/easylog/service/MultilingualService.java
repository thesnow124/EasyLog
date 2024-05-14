package com.github.easylog.service;

import java.util.HashMap;

/**
 * @author Gaosl
 * @project EasyLog
 * @description 获取多语言接口
 * @date 2024/5/14 16:53:46
 */
public interface MultilingualService {

     HashMap<String, String> getMultilingualMap(String key);
}
