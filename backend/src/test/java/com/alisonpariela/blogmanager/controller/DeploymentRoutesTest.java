package com.alisonpariela.blogmanager.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DeploymentRoutesTest {
    @Autowired MockMvc mvc;

    @Test void readinessCanReachDatabaseWithoutAuthentication() throws Exception {
        mvc.perform(get("/healthz")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test void loginRouteForwardsToBundledFrontend() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
