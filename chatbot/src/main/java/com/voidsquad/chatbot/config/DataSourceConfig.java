package com.voidsquad.chatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${DB_URL:jdbc:postgresql://localhost:5432/vector_db}")
    private String url;

    @Value("${DB_USERNAME:postgres}")
    private String username;

    @Value("${DB_PASSWORD:postgres}")
    private String password;

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
