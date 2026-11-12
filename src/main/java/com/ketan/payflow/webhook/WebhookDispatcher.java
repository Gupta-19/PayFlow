package com.ketan.payflow.webhook;

import java.time.Duration;
import java.time.Instant;

import com.ketan.payflow.common.Hashing;
import com.ketan.payflow.config.WebhookProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class WebhookDispatcher {
    private final WebhookProperties props;
    private final RestClient http;

    WebhookDispatcher(WebhookProperties props) {
        this.props = props;
        // A slow or dead receiver must not hold a consumer thread for long
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @KafkaListener(topics = "payment.completed")
    void onPaymentCompleted(String body) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = Hashing.hmacSha256Hex(props.secret(), timestamp + "." + body);
        http.post().uri(props.url())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-PayFlow-Timestamp", timestamp)
                .header("X-PayFlow-Signature", "sha256=" + signature)
                .body(body)
                .retrieve()
                .toBodilessEntity();   // a 4xx or 5xx throws: 5xx/timeouts retry, 4xx goes straight to the DLT
    }
}
