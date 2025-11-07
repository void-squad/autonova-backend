package com.voidsquad.chatbot;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@EnableRabbit
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) //TODO: enable the database connection
public class ChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
    }

}
