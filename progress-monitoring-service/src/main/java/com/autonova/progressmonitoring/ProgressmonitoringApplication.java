package com.autonova.progressmonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@EnableRabbit
@SpringBootApplication
public class ProgressmonitoringApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProgressmonitoringApplication.class, args);
	}

}
