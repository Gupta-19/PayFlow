package com.ketan.payflow;

import org.springframework.boot.SpringApplication;

public class TestPayflowApplication {

	public static void main(String[] args) {
		SpringApplication.from(PayflowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
