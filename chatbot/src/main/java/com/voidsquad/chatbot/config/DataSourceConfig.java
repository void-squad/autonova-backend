package com.voidsquad.chatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${DB_URL}")
    String url;

    @Value("${DB_USERNAME}")
    String username;

    @Value("${DB_PASSWORD}")
    String password;

    @Bean
    public DataSource dataSource() {
        if (url == null || username == null || password == null) {
            String msg = String.format(
                    "Database configuration not found in environment. Checked DB_URL, DB_USERNAME, DB_PASSWORD and spring.datasource.* - found: DB_URL=%s, DB_USERNAME=%s, DB_PASSWORD=%s",
                    url == null ? "<missing>" : "<present>",
                    username == null ? "<missing>" : "<present>",
                    password == null ? "<missing>" : "<present>"
            );
            throw new IllegalStateException(msg);
        }

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}