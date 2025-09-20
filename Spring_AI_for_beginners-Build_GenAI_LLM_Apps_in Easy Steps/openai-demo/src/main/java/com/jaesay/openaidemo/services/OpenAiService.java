package com.jaesay.openaidemo.services;

import com.jaesay.openaidemo.text.prompttemplate.dto.CountryCuisines;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    public OpenAiService(ChatClient.Builder builder, EmbeddingModel embeddingModel) {
        this.chatClient = builder
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
            .build();
        this.embeddingModel = embeddingModel;
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

    public CountryCuisines getCuisines(String country, String numCuisines, String language) {
        PromptTemplate promptTemplate = new PromptTemplate("""
            You are an expert in traditional cuisines.
            You provide information about a specific dish from a specific country.
            Answer the question: What is the traditional cuisine of {country}?
            Return a list of {numCuisines} in {language}.
            Avoid giving information about fictional places. If the country is fictional
            or non-existent answer: I don't know.
            """);

        Prompt prompt = promptTemplate.create(
            Map.of("country", country, "numCuisines", numCuisines, "language", language));
        return chatClient.prompt(prompt).call().entity(CountryCuisines.class);
    }

    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    public double findSimilarity(String text1, String text2) {
        List<float[]> response = embeddingModel.embed(List.of(text1, text2));
        return cosineSimilarity(response.get(0), response.get(1));
    }

    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must be of the same length");
        }

        // Initialize variables for dot product and magnitudes
        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        // Calculate dot product and magnitudes
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            magnitudeA += vectorA[i] * vectorA[i];
            magnitudeB += vectorB[i] * vectorB[i];
        }

        // Calculate and return cosine similarity
        return dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
    }

}
