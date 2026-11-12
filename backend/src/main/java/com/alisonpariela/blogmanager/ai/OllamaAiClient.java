package com.alisonpariela.blogmanager.ai;

import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;

/** Optional local adapter. Selecting hosted mode never contacts or starts Ollama. */
public final class OllamaAiClient implements AiClient {
    private final RestClient client;
    private final String embeddingModel;
    private final String model;

    public OllamaAiClient(String url, String embeddingModel, String model) {
        this.client = AiHttp.client(url, "");
        this.embeddingModel = embeddingModel;
        this.model = model;
    }
    public record Embeddings(List<List<Double>> embeddings) {}
    public record Generation(String response) {}

    @Override
    public List<List<Double>> embed(List<String> input) {
        var result = AiHttp.request(() -> client.post().uri("/api/embed")
                .body(Map.of("model", embeddingModel, "input", input))
                .retrieve().body(Embeddings.class));
        return AiHttp.validate(result == null ? null : result.embeddings(), input.size());
    }

    @Override
    public String generate(String system, String prompt) {
        var result = AiHttp.request(() -> client.post().uri("/api/generate")
                .body(Map.of("model", model, "stream", false, "system", system, "prompt", prompt,
                        "options", Map.of("temperature", .1, "num_predict", 600)))
                .retrieve().body(Generation.class));
        if (result == null || result.response() == null || result.response().isBlank()) {
            throw AiHttp.invalidResponse();
        }
        return result.response();
    }
}
