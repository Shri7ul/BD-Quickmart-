package com.bdquickmart.common.exception;

/** Service-level validation failure that should surface as HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
