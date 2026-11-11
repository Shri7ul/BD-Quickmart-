package com.bdquickmart.modules.auth.exception;

/** Raised when the identifier or password does not match. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
