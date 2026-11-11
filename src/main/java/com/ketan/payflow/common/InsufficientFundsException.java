package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends ApiException {
    public InsufficientFundsException(long balance, long amount) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "insufficient-funds", "Insufficient funds",
              "Balance " + balance + " is less than amount " + amount);
    }
}
