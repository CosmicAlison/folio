package com.alisonpariela.blogmanager.controller;
import com.alisonpariela.blogmanager.model.User;
import com.alisonpariela.blogmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class PostsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Test void crudValidationIsolationAndSerialization() throws Exception {
        var owner = new User(); owner.setUsername("owner"); owner.setEmail("owner@example.com"); owner.setPassword("secret-hash"); owner = users.save(owner);
        var other = new User(); other.setUsername("other"); other.setEmail("other@example.com"); other.setPassword("other-hash"); other = users.save(other);
        mvc.perform(post("/api/posts").with(user(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \",\"contents\":\"\"}")).andExpect(status().isBadRequest());
        var result = mvc.perform(post("/api/posts").with(user(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Design\",\"contents\":\"Accessible design uses labels\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.user").doesNotExist()).andExpect(jsonPath("$.title").value("Design")).andReturn();
        var json = new tools.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
        long id = json.get("id").asLong();
        mvc.perform(get("/api/posts").with(user(other))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(put("/api/posts/"+id).with(user(other)).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Stolen\",\"contents\":\"No\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/posts/"+id).with(user(other))).andExpect(status().isForbidden());
        mvc.perform(post("/api/assistant").with(user(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"design\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.sources[0].postId").value(id));
        mvc.perform(post("/api/assistant").with(user(other)).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"design\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.sources").isEmpty());
        mvc.perform(post("/api/assistant").with(user(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\" \"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/posts/"+id).with(user(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Updated\",\"contents\":\"New content\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated"));
        mvc.perform(delete("/api/posts/"+id).with(user(owner))).andExpect(status().isNoContent());
        mvc.perform(delete("/api/posts/"+id).with(user(owner))).andExpect(status().isNotFound());
    }
}
