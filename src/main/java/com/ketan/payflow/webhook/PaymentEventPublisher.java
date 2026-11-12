package com.ketan.payflow.webhook;

import com.ketan.payflow.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
class PaymentEventPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(PaymentCompletedEvent e) {
        // The money has already moved: a Kafka problem must never turn this request into a 500.
        // Losing the event here is the known gap the transactional outbox would close.
        try {
            // Keyed by the receiving wallet, so one receiver's events stay in order
            kafka.send("payment.completed", e.toWalletId().toString(), json.writeValueAsString(e))
                    .whenComplete((result, ex) -> {
                        if (ex != null) log.warn("Async publish of payment.completed failed for payment {}", e.paymentId(), ex);
                    });
        } catch (RuntimeException ex) {
            log.warn("Publish of payment.completed failed for payment {}", e.paymentId(), ex);
        }
    }
}
