package com.autonova.progressmonitoring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.autonova.progressmonitoring.repository.ProjectMessageRepository;

import static org.mockito.Mockito.mock;

@SpringBootTest(properties = {
		// avoid trying to auto-configure a DataSource during tests that don't need it
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class ProgressmonitoringApplicationTests {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public ProjectMessageRepository projectMessageRepository() {
            return mock(ProjectMessageRepository.class);
        }
    }

	@Test
	void contextLoads() {
	}

}
