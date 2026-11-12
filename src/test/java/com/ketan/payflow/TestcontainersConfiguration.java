package com.ketan.payflow;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);
	}

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		// Kafka 3.9 derives the controller's advertised listener from KAFKA_LISTENERS and rejects the
		// 0.0.0.0 host Testcontainers puts there by default; an empty host binds all interfaces just the same.
		return new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.0"))
				.withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094");
	}

}
