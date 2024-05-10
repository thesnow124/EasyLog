package com.github.easylog.model;

import lombok.Data;

/**
 * @author Gaosl
 */
@Data
public class MethodExecuteResult {

	private boolean success;

	private Throwable throwable;

	private String errMsg;

	private Long operateTime;

	private Long executeTime;

	public MethodExecuteResult(boolean success) {
		this.success = success;
		this.operateTime = System.currentTimeMillis();
	}

    public void calcExecuteTime() {
		this.executeTime = System.currentTimeMillis() - this.operateTime;
	}

	public void exception(Throwable throwable) {
		this.success = false;
		this.executeTime = System.currentTimeMillis() - this.operateTime;
		this.throwable = throwable;
		this.errMsg = throwable.getMessage();
	}

}