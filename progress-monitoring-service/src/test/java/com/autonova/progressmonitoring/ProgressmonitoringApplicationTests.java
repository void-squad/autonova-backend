package com.autonova.progressmonitoring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		// avoid trying to auto-configure a DataSource during tests that don't need it
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class ProgressmonitoringApplicationTests {

	@Test
	void contextLoads() {
	}

}
