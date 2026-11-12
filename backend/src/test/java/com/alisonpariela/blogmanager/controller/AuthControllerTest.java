package com.alisonpariela.blogmanager.controller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @Test void signupRejectsInvalidInput() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"\",\"email\":\"invalid\",\"password\":\"\"}")).andExpect(status().isBadRequest());
    }
    @Test void postsRequireAuthentication() throws Exception { mvc.perform(get("/api/posts")).andExpect(status().isUnauthorized()); }
    @Test void assistantRequiresAuthentication() throws Exception { mvc.perform(post("/api/assistant").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"hello\"}")).andExpect(status().isUnauthorized()); }
    @Test void invalidTokenReturns401() throws Exception { mvc.perform(get("/api/posts").header("Authorization","Bearer garbage")).andExpect(status().isUnauthorized()); }
    @Test void profileRequiresAuthentication() throws Exception { mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized()); }
    @Test void signupAndLoginIssueWorkingAccessTokens() throws Exception {
        String signup = "{\"username\":\"newwriter\",\"email\":\"newwriter@example.com\",\"password\":\"password123\"}";
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup)).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString());
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"newwriter\",\"password\":\"password123\"}"))
            .andExpect(status().isOk()).andReturn();
        var tokens = new tools.jackson.databind.ObjectMapper().readTree(login.getResponse().getContentAsString());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+tokens.get("accessToken").asText()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("newwriter")).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/posts").header("Authorization","Bearer "+tokens.get("refreshToken").asText())).andExpect(status().isUnauthorized());
    }
}
