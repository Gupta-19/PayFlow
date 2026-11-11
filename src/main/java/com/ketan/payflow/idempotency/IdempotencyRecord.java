package com.ketan.payflow.idempotency;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record IdempotencyRecord(String state, String requestHash, int status, String body) {
    static IdempotencyRecord inProgress(String hash) {
        return new IdempotencyRecord("IN_PROGRESS", hash, 0, null);
    }

    static IdempotencyRecord completed(String hash, int status, String body) {
        return new IdempotencyRecord("COMPLETED", hash, status, body);
    }

    @JsonIgnore   // derived from state; must not be written as a "completed" property
    boolean isCompleted() {
        return "COMPLETED".equals(state);
    }
}
