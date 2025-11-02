package com.autonova.employee_dashboard_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class EmployeeDashboardServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmployeeDashboardServiceApplication.class, args);
	}

}
