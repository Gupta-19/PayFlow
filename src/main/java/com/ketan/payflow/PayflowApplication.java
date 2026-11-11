package com.ketan.payflow;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PayflowApplication {

	public static void main(String[] args) {
		// pgjdbc sends the JVM zone on connect; legacy names like Asia/Calcutta are rejected by Postgres 16
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(PayflowApplication.class, args);
	}

}
