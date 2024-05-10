package com.github.easylog.aop;

import com.github.easylog.annotation.EasyLog;
import com.github.easylog.configuration.EasyLogProperties;
import com.github.easylog.function.EasyLogParser;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.model.EasyLogOps;
import com.github.easylog.model.MethodExecuteResult;
import com.github.easylog.service.ILogRecordService;
import com.github.easylog.service.IOperatorService;
import com.github.easylog.util.JsonUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Gaosl
 */
@Aspect
@Component
@Slf4j
public class EasyLogAspect {


	private  ILogRecordService logRecordService;

	private  IOperatorService operatorService;

	private  EasyLogParser easyLogParser;

	private  EasyLogProperties easyLogProperties;

	public EasyLogAspect(ILogRecordService logRecordService, IOperatorService operatorService, EasyLogParser easyLogParser, EasyLogProperties easyLogProperties) {
		this.logRecordService = logRecordService;
		this.operatorService = operatorService;
		this.easyLogParser = easyLogParser;
		this.easyLogProperties = easyLogProperties;
	}

	/**
	 * 定义切点
	 */
	@Pointcut("@annotation(com.github.easylog.annotation.EasyLog) || @annotation(com.github.easylog.annotation.EasyLogs)")
	public void pointCut() {
	}

	/**
	 * 环绕通知
	 *
	 * @param joinPoint joinPoint
	 * @return Object
	 */
	@Around("pointCut()")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		Method method = methodSignature.getMethod();
		Object[] args = joinPoint.getArgs();
		Object target = joinPoint.getTarget();
		Class<?> targetClass = AopUtils.getTargetClass(target);

		EasyLog[] logList = method.getAnnotationsByType(EasyLog.class);

		List<EasyLogOps> easyLogOpsList = new ArrayList<>();
		for (EasyLog log : logList) {
			easyLogOpsList.add(parseLogAnnotation(log));
		}

		List<String> expressTemplate = getExpressTemplate(easyLogOpsList);

		Map<String, String> customFunctionExecResultMap = easyLogParser.processBeforeExec(expressTemplate, method, args, targetClass);

		Object result = null;
		MethodExecuteResult executeResult = new MethodExecuteResult(true);
		try {
			result = joinPoint.proceed();
			executeResult.calcExecuteTime();
		} catch (Throwable e) {
			executeResult.exception(e);
		}

		boolean existsNoFailTemp = easyLogOpsList.stream()
				.anyMatch(easyLogOps -> ObjectUtils.isEmpty(easyLogOps.getFail()));
		if (!executeResult.isSuccess() && existsNoFailTemp) {
			log.warn("[{}] 方法执行失败，EasyLog 失败模板没有配置", method.getName());
		} else {
			Map<String, String> templateMap = easyLogParser.processAfterExec(expressTemplate, customFunctionExecResultMap, method, args, targetClass, executeResult.getErrMsg(), result);
			sendLog(easyLogOpsList, result, executeResult, templateMap);
		}
		//抛出异常
		if (!executeResult.isSuccess()) {
			throw executeResult.getThrowable();
		}
		return result;
	}

	/**
	 * 发送日志
	 *
	 * @param easyLogOps
	 * @param result
	 * @param executeResult
	 * @param templateMap
	 */
	private void sendLog(List<EasyLogOps> easyLogOps, Object result, MethodExecuteResult executeResult, Map<String, String> templateMap) {
		List<EasyLogInfo> easyLogInfos = createEasyLogInfo(templateMap, easyLogOps, executeResult);
		if (!CollectionUtils.isEmpty(easyLogInfos)) {
			easyLogInfos.forEach(easyLogInfo -> {
				easyLogInfo.setPlatform(easyLogProperties.getPlatform());
				easyLogInfo.setResult(JsonUtils.toJSONString(result));
				logRecordService.record(easyLogInfo);
			});
		}
	}

	/**
	 * 创建操作日志实体
	 *
	 * @param templateMap
	 * @param easyLogOpsList
	 * @return
	 */
	private List<EasyLogInfo> createEasyLogInfo(Map<String, String> templateMap, List<EasyLogOps> easyLogOpsList, MethodExecuteResult executeResult) {
		List<EasyLogInfo> easyLogInfos = new ArrayList<>();
		for (EasyLogOps easyLogOps : easyLogOpsList) {
			//记录条件为 false，则不记录
			if (Boolean.FALSE.toString().equalsIgnoreCase(templateMap.get(easyLogOps.getCondition()))) {
				continue;
			}

			EasyLogInfo easyLogInfo = new EasyLogInfo();
			String platform = templateMap.get(easyLogOps.getPlatform());
			if (ObjectUtils.isEmpty(platform)) {
				platform = operatorService.getPlatform();
			}
			easyLogInfo.setPlatform(platform);
			String operator = templateMap.get(easyLogOps.getOperator());
			if (ObjectUtils.isEmpty(operator)) {
				operator = operatorService.getOperator();
			}
			easyLogInfo.setModule(easyLogOps.getModule());
			easyLogInfo.setType(easyLogOps.getType());
			easyLogInfo.setOperator(operator);
			easyLogInfo.setBizNo(templateMap.get(easyLogOps.getBizNo()));
			easyLogInfo.setDetails(templateMap.get(easyLogOps.getDetails()));

			easyLogInfo.setContent(executeResult.isSuccess() ? templateMap.get(easyLogOps.getSuccess()) : templateMap.get(easyLogOps.getFail()));
			easyLogInfo.setSuccess(executeResult.isSuccess());
			easyLogInfo.setErrorMsg(executeResult.getErrMsg());
			easyLogInfo.setExecuteTime(executeResult.getExecuteTime());
			easyLogInfo.setOperateTime(executeResult.getOperateTime());

			easyLogInfos.add(easyLogInfo);
		}

		return easyLogInfos;
	}


	/**
	 * 将注解转为实体
	 *
	 * @param easyLog easyLog
	 * @return EasyLogOps
	 */
	private EasyLogOps parseLogAnnotation(EasyLog easyLog) {
		EasyLogOps easyLogOps = new EasyLogOps();
		easyLogOps.setSuccess(easyLog.success());
		easyLogOps.setSuccessParamList(easyLog.successParamList());
		easyLogOps.setFail(easyLog.fail());
		easyLogOps.setFailParamList(easyLog.failParamList());
		easyLogOps.setModule(easyLog.module());
		easyLogOps.setType(easyLog.type());
		easyLogOps.setOperator(easyLog.operator());
		easyLogOps.setBizNo(easyLog.bizNo());
		easyLogOps.setPlatform(easyLog.platform());
		easyLogOps.setDetails(easyLog.detail());
		easyLogOps.setCondition(easyLog.condition());
		return easyLogOps;
	}

	/**
	 * 获取不为空的 待解析模板
	 *
	 * @param easyLogOpsList easyLogOpsList
	 * @return List<String>
	 */
	private List<String> getExpressTemplate(List<EasyLogOps> easyLogOpsList) {
		Set<String> set = new HashSet<>();
		for (EasyLogOps easyLogOps : easyLogOpsList) {
			set.addAll(Lists.newArrayList(easyLogOps.getBizNo(), easyLogOps.getDetails(),
					easyLogOps.getOperator(), easyLogOps.getPlatform(), easyLogOps.getSuccess(), easyLogOps.getFail(),
					easyLogOps.getCondition()));
		}
		return set.stream()
				.filter(s -> !ObjectUtils.isEmpty(s))
				.collect(Collectors.toList());
	}
}
