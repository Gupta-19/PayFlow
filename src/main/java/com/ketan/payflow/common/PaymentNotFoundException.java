package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends ApiException {
    public PaymentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "payment-not-found", "Payment not found",
              "Payment does not exist");
    }
}
