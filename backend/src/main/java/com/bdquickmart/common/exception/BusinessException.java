package com.bdquickmart.common.exception;

import com.bdquickmart.common.api.ApiCode;

/**
 * Base type for expected, business-rule violations. Carries the HTTP status and
 * application code that the client should observe.
 */
public class BusinessException extends RuntimeException {

    private final ApiCode apiCode;

    public BusinessException(ApiCode apiCode, String message) {
        super(message);
        this.apiCode = apiCode;
    }

    public BusinessException(ApiCode apiCode, String message, Throwable cause) {
        super(message, cause);
        this.apiCode = apiCode;
    }

    public ApiCode getApiCode() {
        return apiCode;
    }
}
