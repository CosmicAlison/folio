package com.alisonpariela.blogmanager.config;

import com.alisonpariela.blogmanager.ai.AiClient;
import com.alisonpariela.blogmanager.ai.HostedAiClient;
import com.alisonpariela.blogmanager.ai.OllamaAiClient;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    public AiClient aiClient(
            @Value("${ai.provider:hosted}") String provider,
            @Value("${ai.url:https://openrouter.ai/api/v1}") String url,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.embedding-model:openai/text-embedding-3-small}") String embeddingModel,
            @Value("${ai.model:google/gemini-2.5-flash-lite}") String model) {
        return switch (provider.trim().toLowerCase(Locale.ROOT)) {
            case "hosted" -> new HostedAiClient(url, apiKey, embeddingModel, model);
            case "ollama" -> new OllamaAiClient(url, embeddingModel, model);
            default -> throw new IllegalArgumentException("AI_PROVIDER must be hosted or ollama");
        };
    }
}
