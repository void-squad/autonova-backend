package com.voidsquad.chatbot;

import com.netflix.discovery.EurekaClient;
import com.voidsquad.chatbot.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class ChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
    }


    //TODO: remove this testing codes for RabbitMQ
//    @RabbitListener(queues = RabbitMQConfig.QUEUE)
//    public void receive(String message) {
//        System.out.println("Received Message: " + message);
//    }

}

