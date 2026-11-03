package com.yicheng.outreach.common;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。
 * <p>HTTP 状态码与业务码分离：HTTP 表达传输语义，业务码表达具体失败原因。</p>
 */
public enum ErrorCode {

    SUCCESS(0, HttpStatus.OK, "ok"),
    PARAM_INVALID(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    NOT_FOUND(40401, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(40901, HttpStatus.CONFLICT, "状态冲突"),
    SERVER_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "服务端异常");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
