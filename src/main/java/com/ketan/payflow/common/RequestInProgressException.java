package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class RequestInProgressException extends ApiException {
    public RequestInProgressException() {
        super(HttpStatus.CONFLICT, "request-in-progress", "Request in progress",
              "A request with this Idempotency-Key is still being processed");
    }
}
