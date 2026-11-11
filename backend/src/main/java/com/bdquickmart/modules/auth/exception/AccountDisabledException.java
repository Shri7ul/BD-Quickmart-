package com.bdquickmart.modules.auth.exception;

/** Raised when a disabled or rejected account attempts to authenticate. */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Account is not active");
    }
}
