package com.gaggle.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaggle.demo.config.SecurityConfig;
import com.gaggle.demo.entity.User;
import com.gaggle.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();
    @MockitoBean UserRepository userRepository;

    private final User alice = User.builder()
        .id(1L).name("Alice").email("alice@school.edu").schoolIdentifier(1L).build();
    private final User bob = User.builder()
        .id(2L).name("Bob").email("bob@school.edu").schoolIdentifier(2L).build();


    @Test
    @WithMockUser
    void listUsers_returnsAll() throws Exception {
        given(userRepository.findAll()).willReturn(List.of(alice, bob));

        mvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Alice"))
            .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    @WithMockUser
    void listUsers_empty_returnsEmptyArray() throws Exception {
        given(userRepository.findAll()).willReturn(List.of());

        mvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listUsers_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getUser_found_returnsUser() throws Exception {
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));

        mvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.email").value("alice@school.edu"))
            .andExpect(jsonPath("$.schoolIdentifier").value(1));
    }

    @Test
    @WithMockUser
    void getUser_notFound_returns404() throws Exception {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        mvc.perform(get("/api/users/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void createUser_returns201AndBody() throws Exception {
        given(userRepository.save(any())).willReturn(alice);

        String body = mapper.writeValueAsString(
            Map.of("name", "Alice", "email", "alice@school.edu", "schoolIdentifier", 1));

        mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.email").value("alice@school.edu"));
    }

    @Test
    void createUser_unauthenticated_returns401() throws Exception {
        String body = mapper.writeValueAsString(
            Map.of("name", "Alice", "email", "alice@school.edu", "schoolIdentifier", 1));

        mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void updateUser_found_returnsUpdated() throws Exception {
        User updated = User.builder()
            .id(1L).name("Alice Smith").email("alice@school.edu").schoolIdentifier(1L).build();

        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(userRepository.save(any())).willReturn(updated);

        String body = mapper.writeValueAsString(
            Map.of("name", "Alice Smith", "email", "alice@school.edu", "schoolIdentifier", 1));

        mvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alice Smith"));
    }

    @Test
    @WithMockUser
    void updateUser_notFound_returns404() throws Exception {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        String body = mapper.writeValueAsString(
            Map.of("name", "Ghost", "email", "ghost@x.com", "schoolIdentifier", 999));

        mvc.perform(put("/api/users/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_asAdmin_returns204() throws Exception {
        given(userRepository.existsById(1L)).willReturn(true);

        mvc.perform(delete("/api/users/1"))
            .andExpect(status().isNoContent());

        verify(userRepository).deleteById(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_asUser_returns403() throws Exception {
        mvc.perform(delete("/api/users/1"))
            .andExpect(status().isForbidden());

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_unauthenticated_returns401() throws Exception {
        mvc.perform(delete("/api/users/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_notFound_returns404() throws Exception {
        given(userRepository.existsById(99L)).willReturn(false);

        mvc.perform(delete("/api/users/99"))
            .andExpect(status().isNotFound());

        verify(userRepository, never()).deleteById(any());
    }
}