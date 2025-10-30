package com.autonova.employee_dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EmployeeDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeDashboardApplication.class, args);
    }

}
