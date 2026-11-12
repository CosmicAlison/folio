package com.alisonpariela.blogmanager.service;
import com.alisonpariela.blogmanager.model.*;
import com.alisonpariela.blogmanager.repository.PostRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BlogAssistantTest {
    PostRepository posts = mock(PostRepository.class);
    BlogAssistant assistant = new BlogAssistant(posts,"http://localhost:1",false,"embed","model");
    @BeforeEach void auth() { var user = new User(); user.setId(7L); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of())); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void chunksOverlapAndPreserveTail() { var text = "x".repeat(1900); var chunks = BlogAssistant.chunks(text); assertEquals(3,chunks.size()); assertEquals(500,chunks.get(2).length()); }
    @Test void cosineHandlesZeroAndOrthogonalVectors() { assertEquals(0,BlogAssistant.cosine(List.of(0d),List.of(1d))); assertEquals(0,BlogAssistant.cosine(List.of(1d,0d),List.of(0d,1d))); assertEquals(1,BlogAssistant.cosine(List.of(2d),List.of(5d)),.0001); }
    @Test void emptyCorpusAbstains() { when(posts.findByUserId(7L)).thenReturn(List.of()); assertEquals("empty",assistant.ask("design").mode()); }
    @Test void retrievesOnlyCurrentUsersPosts() {
        var post = new Post(); post.setId(3L); post.setTitle("Design notes"); post.setContents("Accessible design uses clear labels.");
        when(posts.findByUserId(7L)).thenReturn(List.of(post));
        var answer = assistant.ask("design"); assertEquals("search",answer.mode()); assertEquals(3L,answer.sources().get(0).postId());
        verify(posts).findByUserId(7L); verify(posts,never()).findAll();
        assertEquals("no-evidence",assistant.ask("volcanoes").mode());
        when(posts.findByUserId(7L)).thenReturn(List.of()); assertTrue(assistant.ask("design").sources().isEmpty());
    }
    @Test void embedsRetrievesAndGeneratesAgainstProviderContract() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var prompts = new ArrayList<String>();
        server.createContext("/api/embed", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(body.contains("Design notes")); assertFalse(body.contains("other-user-secret"));
            byte[] bytes = "{\"embeddings\":[[1,0],[1,0]]}".getBytes();
            exchange.getResponseHeaders().set("Content-Type","application/json"); exchange.sendResponseHeaders(200,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.createContext("/api/generate", exchange -> {
            prompts.add(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            byte[] bytes = "{\"response\":\"Use clear labels [3].\"}".getBytes();
            exchange.getResponseHeaders().set("Content-Type","application/json"); exchange.sendResponseHeaders(200,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var post = new Post(); post.setId(3L); post.setTitle("Design notes"); post.setContents("Accessible design uses clear labels.");
            when(posts.findByUserId(7L)).thenReturn(List.of(post));
            var rag = new BlogAssistant(posts,"http://127.0.0.1:"+server.getAddress().getPort(),true,"embed","model");
            var answer = rag.ask("design");
            assertEquals("rag",answer.mode()); assertEquals("Use clear labels [3].",answer.answer());
            assertEquals(3L,answer.sources().get(0).postId());
            assertTrue(prompts.get(0).contains("untrusted data")); assertTrue(prompts.get(0).contains("[3] Design notes"));
        } finally { server.stop(0); }
    }
    @Test void rejectsOversizedCorpusBeforeProviderWork() {
        var post = new Post(); post.setId(1L); post.setTitle("Large"); post.setContents("x".repeat(351000));
        when(posts.findByUserId(7L)).thenReturn(List.of(post));
        assertThrows(IllegalArgumentException.class, () -> assistant.ask("x"));
    }
}
