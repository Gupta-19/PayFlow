package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class EmailTakenException extends ApiException {
    public EmailTakenException() {
        super(HttpStatus.CONFLICT, "email-taken", "Email already registered",
              "An account with this email already exists");
    }
}
