package com.jaesay.openaidemo.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService {

    private final ChatClient chatClient;

    public OpenAiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public ChatResponse generateAnswer(String question) {
        return chatClient.prompt(question).call().chatResponse();
    }
}
