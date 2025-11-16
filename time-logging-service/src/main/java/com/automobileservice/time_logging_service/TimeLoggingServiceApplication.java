package com.automobileservice.time_logging_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TimeLoggingServiceApplication {

    public static void main(String[] args) {
        // Load environment variables from infra/.env file
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("../infra")  // Path relative to time-logging-service folder
                    .ignoreIfMissing()      // Don't fail if .env doesn't exist
                    .load();
            
            // Set environment variables as system properties
            dotenv.entries().forEach(entry -> 
                System.setProperty(entry.getKey(), entry.getValue())
            );
            
            System.out.println("[Info] Loaded environment variables from infra/.env");
        } catch (Exception e) {
            System.err.println("[Warning] Could not load .env file: " + e.getMessage());
            System.err.println("[Info] Using default values from application.properties");
        }
        
        SpringApplication.run(TimeLoggingServiceApplication.class, args);
    }
}