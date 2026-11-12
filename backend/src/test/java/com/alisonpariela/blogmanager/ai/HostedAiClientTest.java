package com.alisonpariela.blogmanager.ai;

import com.alisonpariela.blogmanager.model.Post;
import com.alisonpariela.blogmanager.model.User;
import com.alisonpariela.blogmanager.repository.PostRepository;
import com.alisonpariela.blogmanager.service.BlogAssistant;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HostedAiClientTest {
    HttpServer server;
    HostedAiClient client;
    AtomicInteger embeddingStatus;
    AtomicInteger completionStatus;
    String embeddingBody;
    String completionBody;
    List<String> requests;
    List<String> authorization;

    @BeforeEach
    void start() throws Exception {
        embeddingStatus = new AtomicInteger(200);
        completionStatus = new AtomicInteger(200);
        embeddingBody = "{\"data\":[{\"index\":1,\"embedding\":[0.8,0.2]},{\"index\":0,\"embedding\":[1,0]}],\"usage\":{\"prompt_tokens\":12}}";
        completionBody = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Use clear labels [3].\"},\"finish_reason\":\"stop\"}]}";
        requests = new ArrayList<>();
        authorization = new ArrayList<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.add(exchange.getRequestHeaders().getFirst("Authorization"));
            boolean embedding = exchange.getRequestURI().getPath().equals("/api/v1/embeddings");
            byte[] body = (embedding ? embeddingBody : completionBody).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(embedding ? embeddingStatus.get() : completionStatus.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        client = new HostedAiClient(url(), "test-only-key", "embed-model", "chat-model");
    }

    String url() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/"; }

    @AfterEach
    void stop() {
        server.stop(0);
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendsBearerAuthenticatedRequestsAndRestoresEmbeddingOrder() {
        var vectors = client.embed(List.of("question", "passage"));
        assertEquals(List.of(1d, 0d), vectors.get(0));
        assertEquals(List.of(.8d, .2d), vectors.get(1));
        assertEquals("Use clear labels [3].", client.generate("Only use evidence", "A passage"));
        assertEquals(List.of("Bearer test-only-key", "Bearer test-only-key"), authorization);
        assertTrue(requests.get(0).contains("embed-model"));
        assertTrue(requests.get(0).contains("encoding_format"));
        assertTrue(requests.get(1).contains("chat-model"));
        assertTrue(requests.get(1).contains("max_tokens"));
        assertTrue(requests.get(1).contains("Only use evidence"));
    }

    @Test
    void missingKeyFailsWithoutMakingANetworkRequest() {
        var missing = new HostedAiClient(url(), " ", "embed", "chat");
        var error = assertThrows(AiProviderException.class, () -> missing.embed(List.of("text")));
        assertTrue(error.getMessage().contains("AI_API_KEY"));
        assertThrows(AiProviderException.class, () -> missing.generate("system", "prompt"));
        assertTrue(requests.isEmpty());
    }

    @Test
    void errorsDoNotExposeProviderBodiesCredentialsOrBlogText() {
        embeddingStatus.set(401);
        embeddingBody = "{\"error\":{\"message\":\"test-only-key and private-blog-text\"}}";
        var error = assertThrows(AiProviderException.class, () -> client.embed(List.of("private-blog-text")));
        assertTrue(error.getMessage().contains("credentials"));
        assertFalse(error.getMessage().contains("test-only-key"));
        assertFalse(error.getMessage().contains("private-blog-text"));
        embeddingStatus.set(429);
        assertTrue(assertThrows(AiProviderException.class, () -> client.embed(List.of("x"))).getMessage().contains("rate limit"));
        embeddingStatus.set(402);
        assertTrue(assertThrows(AiProviderException.class, () -> client.embed(List.of("x"))).getMessage().contains("credits"));
        embeddingStatus.set(503);
        assertTrue(assertThrows(AiProviderException.class, () -> client.embed(List.of("x"))).getMessage().contains("temporarily unavailable"));
    }

    @Test
    void rejectsMissingDuplicateAndInconsistentVectors() {
        for (String body : List.of(
                "{\"data\":[]}",
                "{\"data\":[{\"index\":0,\"embedding\":[1,0]},{\"index\":0,\"embedding\":[1,0]}]}",
                "{\"data\":[{\"index\":0,\"embedding\":[1]},{\"index\":1,\"embedding\":[1,0]}]}",
                "{\"data\":[{\"index\":0,\"embedding\":[0,0]},{\"index\":1,\"embedding\":[1,0]}]}")) {
            embeddingBody = body;
            assertThrows(AiProviderException.class, () -> client.embed(List.of("question", "passage")));
        }
    }

    @Test
    void rejectsEmptyCompletionAndInvalidJson() {
        completionBody = "{\"choices\":[]}";
        assertThrows(AiProviderException.class, () -> client.generate("system", "prompt"));
        completionBody = "not-json";
        assertThrows(AiProviderException.class, () -> client.generate("system", "prompt"));
    }

    @Test
    void runsTheHostedRetrievalPipelineWithUserScopedSources() {
        var user = new User(); user.setId(7L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var posts = mock(PostRepository.class);
        var post = new Post(); post.setId(3L); post.setTitle("Design notes"); post.setContents("Accessible design uses clear labels.");
        when(posts.findByUserId(7L)).thenReturn(List.of(post));
        var answer = new BlogAssistant(posts, client, true).ask("design");
        assertEquals("rag", answer.mode());
        assertEquals("Use clear labels [3].", answer.answer());
        assertEquals(3L, answer.sources().get(0).postId());
        assertTrue(requests.get(1).contains("untrusted data"));
        assertTrue(requests.get(1).contains("[3] Design notes"));
        verify(posts).findByUserId(7L);
        verify(posts, never()).findAll();
    }
}
