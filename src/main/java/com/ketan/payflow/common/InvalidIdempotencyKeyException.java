package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class InvalidIdempotencyKeyException extends ApiException {
    public InvalidIdempotencyKeyException() {
        super(HttpStatus.BAD_REQUEST, "invalid-idempotency-key", "Invalid idempotency key",
              "Idempotency-Key must be non-blank and at most 100 characters");
    }
}
