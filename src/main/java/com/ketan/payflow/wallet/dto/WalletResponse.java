package com.ketan.payflow.wallet.dto;

import java.util.UUID;

import com.ketan.payflow.wallet.Wallet;

public record WalletResponse(UUID walletId, long balance, String currency) {
    public static WalletResponse from(Wallet w) {
        return new WalletResponse(w.getId(), w.getBalance(), w.getCurrency());
    }
}
