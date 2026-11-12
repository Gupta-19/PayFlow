package com.ketan.payflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.web.client.HttpClientErrorException;

@Configuration
class KafkaConfig {

    @Bean
    NewTopic paymentCompleted() {
        return TopicBuilder.name("payment.completed").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic paymentCompletedDlt() {
        return TopicBuilder.name("payment.completed.dlt").partitions(3).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition("payment.completed.dlt", record.partition()));
        var backOff = new ExponentialBackOffWithMaxRetries(3);   // waits 1 s, 2 s, 4 s
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        var handler = new DefaultErrorHandler(recoverer, backOff);
        // A 4xx means the receiver rejected the request (bad signature, unknown endpoint): retrying won't help,
        // so it goes straight to the DLT. Trade-off: 408 and 429 are transient but also skip the retries.
        handler.addNotRetryableExceptions(HttpClientErrorException.class);
        return handler;
    }
}
