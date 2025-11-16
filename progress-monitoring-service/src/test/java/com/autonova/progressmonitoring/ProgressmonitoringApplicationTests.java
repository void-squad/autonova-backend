package com.autonova.progressmonitoring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;

import com.autonova.progressmonitoring.repository.ProjectMessageRepository;
import com.autonova.progressmonitoring.messaging.publisher.EventPublisher;
import com.autonova.progressmonitoring.messaging.mapper.EventMessageMapper;
import com.autonova.progressmonitoring.service.ProjectMessageService;
import com.autonova.progressmonitoring.messaging.adapter.DomainEventAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@SpringBootTest(properties = {
    // avoid trying to auto-configure a DataSource and JPA during tests that don't need them
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
class ProgressmonitoringApplicationTests {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public ProjectMessageRepository projectMessageRepository() {
            return mock(ProjectMessageRepository.class);
        }

        @Bean
        @Qualifier("testEventPublisher")  // Specify the bean name to resolve the ambiguity
        @Primary
        public EventPublisher eventPublisher() {
            return mock(EventPublisher.class);
        }

        @Bean
        @Primary
        public EventMessageMapper eventMessageMapper() {
            return mock(EventMessageMapper.class);
        }

        @Bean
        public ProjectMessageService projectMessageService() {
            return mock(ProjectMessageService.class);
        }

        @Bean
        public DomainEventAdapter domainEventAdapter() {
            return mock(DomainEventAdapter.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void contextLoads() {
    }
}
