package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid credentials",
              "Email, password or refresh token is invalid");
    }
}
