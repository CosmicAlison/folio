package com.alisonpariela.blogmanager.ai;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class AiHttp {
    private AiHttp() {}

    static RestClient client(String url, String apiKey) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(90000);
        var builder = RestClient.builder().baseUrl(url.replaceAll("/+$", ""))
                .requestFactory(factory);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey.trim());
        }
        return builder.build();
    }

    static <T> T request(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RestClientResponseException ex) {
            // Provider error bodies can echo prompts or credentials. Never log or return them.
            int status = ex.getStatusCode().value();
            log.warn("AI provider request failed with HTTP {}", status);
            String message = switch (status) {
                case 401, 403 -> "The AI provider rejected the server credentials.";
                case 402 -> "The AI provider account has no available credits.";
                case 429 -> "The AI provider rate limit was reached. Please try again later.";
                case 404 -> "The configured AI model or endpoint is unavailable.";
                default -> "The AI provider is temporarily unavailable. Please try again.";
            };
            throw new AiProviderException(message);
        } catch (RestClientException ex) {
            log.warn("AI provider connection or response failed: {}", ex.getClass().getSimpleName());
            throw new AiProviderException("Could not reach the AI provider. Please try again.");
        }
    }

    static List<List<Double>> validate(List<List<Double>> vectors, int expectedCount) {
        if (vectors == null || vectors.size() != expectedCount || vectors.isEmpty()) {
            throw invalidResponse();
        }
        int dimensions = vectors.get(0) == null ? 0 : vectors.get(0).size();
        if (dimensions == 0) throw invalidResponse();
        for (var vector : vectors) {
            if (vector == null || vector.size() != dimensions ||
                    vector.stream().anyMatch(value -> value == null || !Double.isFinite(value)) ||
                    vector.stream().allMatch(value -> value == 0)) {
                throw invalidResponse();
            }
        }
        return vectors;
    }

    static AiProviderException invalidResponse() {
        return new AiProviderException("The AI provider returned an invalid response. Please try again.");
    }
}
