package com.yicheng.outreach.common;

import lombok.Getter;

/**
 * 业务异常。由 {@link GlobalExceptionHandler} 统一转换为标准响应。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object data;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BizException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public static BizException paramInvalid(String field, String message) {
        return new BizException(ErrorCode.PARAM_INVALID, "参数校验失败", java.util.Map.of(field, message));
    }

    public static BizException paramInvalid(java.util.Map<String, String> errors) {
        return new BizException(ErrorCode.PARAM_INVALID, "参数校验失败", errors);
    }

    public static BizException notFound(String message) {
        return new BizException(ErrorCode.NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }
}
