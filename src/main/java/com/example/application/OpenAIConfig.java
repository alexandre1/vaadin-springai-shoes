package com.example.application;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Value("${spring.ai.openai.api-key")
    private String apiKey;

    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }

    @Bean
    public HttpPost deepSeekRequest(@Value("${openai.url}") String apiUrl) {
        HttpPost request = new HttpPost(apiUrl);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", "Bearer " + "sk-proj-hK69CGSjeaP-PXYJgP2VGl6wKRlQUmMzPDdEK2kwhWz3dy9eehYO3Jhq1ttDBxhTGxDLulZ_HIT3BlbkFJE0J0qlAIA1fOsrRNrWH7mlNWzwL2jdXm9AS4QnDoGZUlsxzZ7UrSf3-Gi6UtDiDzzFVBfDJOMAapiKey");
        return request;
    }
}