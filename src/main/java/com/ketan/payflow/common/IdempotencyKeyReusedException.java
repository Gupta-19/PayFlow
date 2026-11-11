package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class IdempotencyKeyReusedException extends ApiException {
    public IdempotencyKeyReusedException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency-key-reused", "Idempotency key reused",
              "This Idempotency-Key was already used with a different request body");
    }
}
