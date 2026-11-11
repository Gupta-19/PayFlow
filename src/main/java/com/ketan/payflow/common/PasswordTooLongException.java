package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class PasswordTooLongException extends ApiException {
    public PasswordTooLongException() {
        super(HttpStatus.BAD_REQUEST, "password-too-long", "Password too long",
              "Password must be at most 72 bytes");
    }
}
