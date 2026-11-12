package com.alisonpariela.blogmanager.controller;

import com.alisonpariela.blogmanager.ai.AiProviderException;
import com.alisonpariela.blogmanager.exception.GlobalExceptionHandler;
import com.alisonpariela.blogmanager.service.BlogAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AssistantErrorsTest {
    @Test
    void providerFailureReturnsAnActionable503() throws Exception {
        var assistant = mock(BlogAssistant.class);
        when(assistant.ask("design")).thenThrow(new AiProviderException("The AI provider account has no available credits."));
        var mvc = MockMvcBuilders.standaloneSetup(new AssistantController(assistant))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/assistant").contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"design\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("The AI provider account has no available credits."));
    }
}
