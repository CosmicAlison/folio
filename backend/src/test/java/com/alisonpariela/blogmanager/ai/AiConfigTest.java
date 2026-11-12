package com.alisonpariela.blogmanager.ai;

import com.alisonpariela.blogmanager.config.AiConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiConfigTest {
    @Test
    void hostedAndLocalProvidersAreExplicitAndInvalidProvidersFailFast() {
        var config = new AiConfig();
        assertInstanceOf(HostedAiClient.class, config.aiClient("hosted", "https://example.invalid", "", "embed", "chat"));
        assertInstanceOf(OllamaAiClient.class, config.aiClient("ollama", "http://localhost:11434", "", "embed", "chat"));
        assertThrows(IllegalArgumentException.class, () -> config.aiClient("invalid", "https://example.invalid", "", "embed", "chat"));
    }
}
