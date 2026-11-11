package com.ketan.payflow.payment;

import java.util.UUID;

import com.ketan.payflow.common.AmountTooLargeException;
import com.ketan.payflow.common.PaymentNotFoundException;
import com.ketan.payflow.common.RecipientNotFoundException;
import com.ketan.payflow.common.SelfPaymentException;
import com.ketan.payflow.common.WalletNotFoundException;
import com.ketan.payflow.config.PaymentProperties;
import com.ketan.payflow.ledger.LedgerService;
import com.ketan.payflow.ledger.MoveCommand;
import com.ketan.payflow.payment.dto.PayRequest;
import com.ketan.payflow.payment.dto.PaymentResponse;
import com.ketan.payflow.wallet.Wallet;
import com.ketan.payflow.wallet.WalletKind;
import com.ketan.payflow.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final WalletRepository wallets;
    private final PaymentRepository payments;
    private final LedgerService ledger;
    private final PaymentProperties props;

    public PaymentResponse pay(UUID userId, PayRequest req, String idempotencyKey) {
        if (req.amount() > props.maxAmount()) throw new AmountTooLargeException(props.maxAmount());
        Wallet from = wallets.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
        Wallet to = wallets.findById(req.toWalletId())
                .filter(w -> w.getKind() == WalletKind.USER)   // nobody can pay the SYSTEM wallet
                .orElseThrow(RecipientNotFoundException::new);
        if (from.getId().equals(to.getId())) throw new SelfPaymentException();

        Payment p = ledger.move(new MoveCommand(PaymentType.TRANSFER, from.getId(), to.getId(),
                req.amount(), userId, idempotencyKey, req.note()));
        return PaymentResponse.from(p);
    }

    public PaymentResponse get(UUID userId, UUID paymentId) {
        UUID mine = wallets.findByUserId(userId).orElseThrow(WalletNotFoundException::new).getId();
        return payments.findById(paymentId)
                .filter(p -> p.getFromWalletId().equals(mine) || p.getToWalletId().equals(mine))
                .map(PaymentResponse::from)
                .orElseThrow(PaymentNotFoundException::new);   // 404 for other people's payments too
    }
}
