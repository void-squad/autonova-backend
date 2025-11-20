package com.voidsquad.chatbot;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Disabled until database configuration is properly mocked in tests")
class ChatbotApplicationTests {

    @Test
    void contextLoads() {
    }



}
