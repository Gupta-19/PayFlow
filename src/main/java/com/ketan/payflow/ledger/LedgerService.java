package com.ketan.payflow.ledger;

import java.util.UUID;

import com.ketan.payflow.common.WalletNotFoundException;
import com.ketan.payflow.payment.Payment;
import com.ketan.payflow.payment.PaymentCompletedEvent;
import com.ketan.payflow.payment.PaymentRepository;
import com.ketan.payflow.wallet.Wallet;
import com.ketan.payflow.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final WalletRepository wallets;
    private final PaymentRepository payments;
    private final LedgerEntryRepository entries;
    private final ApplicationEventPublisher events;

    /**
     * Moves money between two wallets in one transaction: locks both rows, debits, credits,
     * and records the payment and its two ledger entries. The only code that changes a balance.
     * <p>
     * Callers must NOT be {@code @Transactional}. If they were, wallets already loaded into the
     * caller's persistence context would be returned stale by {@code findByIdForUpdate}, and the
     * commit (where the unique-constraint violation used as Step 8's backstop surfaces) must
     * happen inside this method.
     */
    @Transactional
    public Payment move(MoveCommand cmd) {
        if (cmd.amount() <= 0) throw new IllegalArgumentException("amount must be positive");
        if (cmd.fromWalletId().equals(cmd.toWalletId())) {
            throw new IllegalArgumentException("fromWalletId and toWalletId must differ");
        }

        // 1. Lock both wallet rows, smaller id first. Two opposite transfers (A->B and B->A)
        //    then always lock in the same order and can never deadlock.
        UUID first  = cmd.fromWalletId().compareTo(cmd.toWalletId()) < 0 ? cmd.fromWalletId() : cmd.toWalletId();
        UUID second = first.equals(cmd.fromWalletId()) ? cmd.toWalletId() : cmd.fromWalletId();
        Wallet a = wallets.findByIdForUpdate(first).orElseThrow(WalletNotFoundException::new);
        Wallet b = wallets.findByIdForUpdate(second).orElseThrow(WalletNotFoundException::new);
        Wallet from = a.getId().equals(cmd.fromWalletId()) ? a : b;
        Wallet to   = (from == a) ? b : a;

        // 2. Move the money. debit() throws InsufficientFundsException, which rolls everything back.
        from.debit(cmd.amount());
        to.credit(cmd.amount());

        // 3. Record the payment and its two ledger entries
        // saveAndFlush so a duplicate (created_by, idempotency_key) fails right here as DataIntegrityViolationException
        Payment payment = payments.saveAndFlush(Payment.completed(cmd));
        entries.save(LedgerEntry.debit(payment.getId(), from.getId(), cmd.amount()));
        entries.save(LedgerEntry.credit(payment.getId(), to.getId(), cmd.amount()));

        // 4. Announce it. PaymentEventPublisher sends to Kafka only AFTER_COMMIT, so a rollback is never announced.
        events.publishEvent(new PaymentCompletedEvent(payment.getId(), from.getId(), to.getId(),
                cmd.amount(), payment.getCreatedAt()));

        // 5. The new balances are written at commit (JPA dirty checking)
        return payment;
    }
}
