package com.jaesay.openaidemo.services;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService {

    private final ChatClient chatClient;

    public OpenAiService(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
            .build();
    }

    public ChatResponse generateAnswer(String question) {
        return chatClient.prompt(question).call().chatResponse();
    }

    public String getTravelGuide(String city, String month, String language, String budget) {
        PromptTemplate promptTemplate = new PromptTemplate("""
            Welcome to the {city} travel guide!
            If you're visiting in {month}, here's what you can do: 1. Must-visit attractions.
            2. Local cuisine you must try.
            3. Useful phrases in {language}.
            4. Tips for traveling on a {budget} budget.
            Enjoy your trip!
            """);

        Prompt prompt = promptTemplate.create(
            Map.of("city", city, "month", month, "language", language, "budget", budget));

        return chatClient.prompt(prompt).call().chatResponse().getResult().getOutput().getText();
    }
}
