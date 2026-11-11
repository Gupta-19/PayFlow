package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class RecipientNotFoundException extends ApiException {
    public RecipientNotFoundException() {
        super(HttpStatus.NOT_FOUND, "recipient-not-found", "Recipient not found",
              "Recipient wallet does not exist");
    }
}
