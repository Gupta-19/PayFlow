package com.ketan.payflow.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.ketan.payflow.common.Hashing;
import com.ketan.payflow.config.WebhookProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/** A stand-in merchant that verifies PayFlow's webhooks. Only on when app.webhook.demo-receiver=true. */
@Slf4j
@RestController
@RequestMapping("/demo/merchant-webhook")
@ConditionalOnProperty(prefix = "app.webhook", name = "demo-receiver", havingValue = "true")
class DemoMerchantController {
    private static final int MAX_SEEN = 1_000;

    private final WebhookProperties props;
    private final JsonMapper json;
    private volatile boolean failing = false;

    // Webhooks are at-least-once, so a receiver de-duplicates by paymentId. Bounded: oldest ids drop out.
    private final Set<String> seenPaymentIds = Collections.synchronizedSet(Collections.newSetFromMap(
            new LinkedHashMap<String, Boolean>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_SEEN;
                }
            }));

    DemoMerchantController(WebhookProperties props, JsonMapper json) {
        this.props = props;
        this.json = json;
    }

    @PostMapping
    ResponseEntity<Void> receive(@RequestHeader("X-PayFlow-Timestamp") String timestamp,
                                 @RequestHeader("X-PayFlow-Signature") String signature,
                                 @RequestBody String body) {
        if (failing) return ResponseEntity.status(503).build();
        long sentAt;
        try {
            sentAt = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401).build();
        }
        boolean fresh = Math.abs(Instant.now().getEpochSecond() - sentAt) <= 300;
        String expected = "sha256=" + Hashing.hmacSha256Hex(props.secret(), timestamp + "." + body);
        boolean valid = MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));   // constant time
        if (!fresh || !valid) return ResponseEntity.status(401).build();

        String paymentId = json.readTree(body).path("paymentId").asString();
        if (!seenPaymentIds.add(paymentId)) {
            log.info("Duplicate webhook ignored: payment {}", paymentId);
            return ResponseEntity.ok().build();
        }
        log.info("Webhook received: {}", body);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/failing")
    void setFailing(@RequestParam boolean on) {
        failing = on;
    }
}
