package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class SelfPaymentException extends ApiException {
    public SelfPaymentException() {
        super(HttpStatus.BAD_REQUEST, "self-payment", "Self payment not allowed",
              "You cannot pay your own wallet");
    }
}
