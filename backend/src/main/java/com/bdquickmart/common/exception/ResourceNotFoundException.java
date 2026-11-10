package com.bdquickmart.common.exception;

import com.bdquickmart.common.api.ApiCode;

/**
 * Raised when a requested resource does not exist. Translated to HTTP 404 by
 * {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final String fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public ApiCode getApiCode() {
        return ApiCode.NOT_FOUND;
    }
}
