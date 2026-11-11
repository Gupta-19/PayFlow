package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class AmountTooLargeException extends ApiException {
    public AmountTooLargeException(long max) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "amount-too-large", "Amount too large",
              "Amount exceeds the maximum of " + max + " paise per payment");
    }
}
