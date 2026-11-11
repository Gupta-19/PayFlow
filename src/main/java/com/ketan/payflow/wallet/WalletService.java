package com.ketan.payflow.wallet;

import java.util.UUID;

import com.ketan.payflow.common.AmountTooLargeException;
import com.ketan.payflow.common.PageResponse;
import com.ketan.payflow.common.WalletNotFoundException;
import com.ketan.payflow.config.PaymentProperties;
import com.ketan.payflow.ledger.LedgerEntryRepository;
import com.ketan.payflow.ledger.LedgerService;
import com.ketan.payflow.ledger.MoveCommand;
import com.ketan.payflow.payment.Payment;
import com.ketan.payflow.payment.PaymentType;
import com.ketan.payflow.payment.dto.PaymentResponse;
import com.ketan.payflow.wallet.dto.LedgerEntryResponse;
import com.ketan.payflow.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository wallets;
    private final LedgerEntryRepository entries;
    private final LedgerService ledger;
    private final PaymentProperties props;

    public WalletResponse get(UUID userId) {
        return WalletResponse.from(myWallet(userId));
    }

    public PageResponse<LedgerEntryResponse> history(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100));
        return PageResponse.from(entries
                .findByWalletIdOrderByCreatedAtDesc(myWallet(userId).getId(), pageable)
                .map(LedgerEntryResponse::from));
    }

    public PaymentResponse topUp(UUID userId, long amount, String idempotencyKey) {
        if (amount > props.maxAmount()) throw new AmountTooLargeException(props.maxAmount());
        Wallet mine = myWallet(userId);
        Payment p = ledger.move(new MoveCommand(PaymentType.TOPUP, Wallet.SYSTEM_WALLET_ID, mine.getId(),
                amount, userId, idempotencyKey, "Top-up"));
        return PaymentResponse.from(p);
    }

    private Wallet myWallet(UUID userId) {
        return wallets.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
    }
}
