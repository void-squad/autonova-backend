package com.voidsquad.chatbot.service.language.provider;


public interface ChatResponseWrapper {
    ChatResponseWrapper system(String text);
    ChatResponseWrapper user(String text);
    ChatResponse call();
}
