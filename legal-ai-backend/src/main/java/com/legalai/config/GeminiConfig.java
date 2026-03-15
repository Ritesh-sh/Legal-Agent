package com.legalai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta}")
    private String apiUrl;

    @Value("${gemini.model:gemma-3-4b-it}")
    private String model;

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(apiUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public String getApiKey() { return apiKey; }
    public String getApiUrl() { return apiUrl; }
    public String getModel() { return model; }
}
