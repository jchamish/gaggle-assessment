package com.gaggle.demo.controller;

import com.gaggle.demo.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicController.class)
@Import(SecurityConfig.class)
class PublicControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void info_noAuth_returns200() throws Exception {
        mvc.perform(get("/api/public/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.app").value("Gaggle Demo"))
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser
    void info_authenticated_returns200() throws Exception {
        mvc.perform(get("/api/public/info"))
            .andExpect(status().isOk());
    }
}