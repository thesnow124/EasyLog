package com.github.easylog.model;


import com.github.easylog.compare.FieldInfo;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author Gaosl
 */
@Data
public class EasyLogInfo {

	/**
	 * ip
	 */
	private String ip;

	/**
	 * url
	 */
	private String url;

	/**
	 * HTTP请求方式
	 */
	private String httpMethod;

	/**
	 * 类.方法
	 */
	private String classMethod;

	/**
	 * 接口参数
	 */
	private Map<String, Object> param;


	/**
	 * 平台
	 */
	private String platform;

	/**
	 * 操作者
	 */
	private String operator;

	/**
	 * 操作时间 时间戳单位：ms
	 */
	private Long operateTime;

	/**
	 * 业务id
	 */
	private String bizNo;

	/**
	 * 模块
	 */
	private String module;

	/**
	 * 操作类型
	 */
	private String type;

	/**
	 * 操作内容
	 */
	private String content;

	/**
	 * 操作参数
	 */
	private String[] contentParam;

	/**
	 * 操作花费的时间 单位：ms
	 */
	private Long executeTime;

	/**
	 * 是否调用成功
	 */
	private Boolean success;

	/**
	 * 执行后返回的json字符串
	 */
	private String result;

	private String errorMsg;

	/**
	 * 异常堆栈信息
	 */
	private String stackTrace;


	/**
	 * 详细
	 */
	private String detail;

	/**
	 * 详细的字段变更
	 */
	private List<FieldInfo> fieldInfoList;

	/**
	 * 记录条件
	 */
	private String condition;

}
