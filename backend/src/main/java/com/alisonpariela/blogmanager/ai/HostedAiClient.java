package com.alisonpariela.blogmanager.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;

/** OpenAI-compatible wire format, configured for OpenRouter by default. No SDK required. */
public final class HostedAiClient implements AiClient {
    private final RestClient client;
    private final boolean hasApiKey;
    private final String embeddingModel;
    private final String model;

    public HostedAiClient(String url, String apiKey, String embeddingModel, String model) {
        this.client = AiHttp.client(url, apiKey);
        this.hasApiKey = apiKey != null && !apiKey.isBlank();
        this.embeddingModel = embeddingModel;
        this.model = model;
    }

    public record Embedding(Integer index, List<Double> embedding) {}
    public record Embeddings(List<Embedding> data) {}
    public record Message(String role, String content) {}
    public record Choice(Message message) {}
    public record Completion(List<Choice> choices) {}

    private void requireKey() {
        if (!hasApiKey) {
            throw new AiProviderException("Hosted AI is not configured. The server needs an AI_API_KEY.");
        }
    }

    @Override
    public List<List<Double>> embed(List<String> input) {
        requireKey();
        var result = AiHttp.request(() -> client.post().uri("/embeddings")
                .body(Map.of("model", embeddingModel, "input", input, "encoding_format", "float"))
                .retrieve().body(Embeddings.class));
        if (result == null || result.data() == null || result.data().size() != input.size()) {
            throw AiHttp.invalidResponse();
        }
        // Index is authoritative; providers need not return vectors in input order.
        var ordered = new ArrayList<List<Double>>(Collections.nCopies(input.size(), null));
        for (var item : result.data()) {
            if (item == null || item.index() == null || item.index() < 0 ||
                    item.index() >= input.size() || ordered.get(item.index()) != null || item.embedding() == null) {
                throw AiHttp.invalidResponse();
            }
            ordered.set(item.index(), item.embedding());
        }
        return AiHttp.validate(ordered, input.size());
    }

    @Override
    public String generate(String system, String prompt) {
        requireKey();
        var result = AiHttp.request(() -> client.post().uri("/chat/completions")
                .body(Map.of("model", model, "stream", false, "max_tokens", 600, "temperature", .1,
                        "messages", List.of(new Message("system", system), new Message("user", prompt))))
                .retrieve().body(Completion.class));
        if (result == null || result.choices() == null || result.choices().isEmpty() ||
                result.choices().get(0) == null || result.choices().get(0).message() == null ||
                result.choices().get(0).message().content() == null ||
                result.choices().get(0).message().content().isBlank()) {
            throw AiHttp.invalidResponse();
        }
        return result.choices().get(0).message().content();
    }
}
