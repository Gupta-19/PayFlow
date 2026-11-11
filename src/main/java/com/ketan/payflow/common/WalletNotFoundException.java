package com.ketan.payflow.common;

import org.springframework.http.HttpStatus;

public class WalletNotFoundException extends ApiException {
    public WalletNotFoundException() {
        super(HttpStatus.NOT_FOUND, "wallet-not-found", "Wallet not found",
              "No wallet exists for the current user");
    }
}
