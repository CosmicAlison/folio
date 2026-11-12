package com.alisonpariela.blogmanager.controller;
import com.alisonpariela.blogmanager.service.BlogAssistant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final BlogAssistant assistant;
    public AssistantController(BlogAssistant assistant) { this.assistant = assistant; }
    public record Question(@NotBlank @Size(max=1000) String question) {}
    @PostMapping
    public BlogAssistant.Answer ask(@Valid @RequestBody Question question) { return assistant.ask(question.question()); }
}
