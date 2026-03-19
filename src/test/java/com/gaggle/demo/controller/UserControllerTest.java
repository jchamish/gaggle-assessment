package com.gaggle.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaggle.demo.config.SecurityConfig;
import com.gaggle.demo.entity.User;
import com.gaggle.demo.exception.GlobalExceptionHandler;
import com.gaggle.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();
    @MockitoBean UserService userService;

    private final User alice = User.builder()
        .id(1L).name("Alice").email("alice@school.edu").schoolIdentifier(1L).build();
    private final User bob = User.builder()
        .id(2L).name("Bob").email("bob@school.edu").schoolIdentifier(2L).build();

    // ── GET /api/users ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void listUsers_returnsAll() throws Exception {
        given(userService.listAll(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(alice, bob)));

        mvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].name").value("Alice"))
            .andExpect(jsonPath("$.content[1].name").value("Bob"))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser
    void listUsers_empty_returnsEmptyArray() throws Exception {
        given(userService.listAll(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser
    void listUsers_customPage_passesPageableToRepo() throws Exception {
        given(userService.listAll(any(Pageable.class)))
            .willAnswer(inv -> new PageImpl<>(List.of(alice), inv.getArgument(0), 1));

        mvc.perform(get("/api/users?page=0&size=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void listUsers_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    // ── GET /api/users/{id} ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getUser_found_returnsUser() throws Exception {
        given(userService.getById(1L)).willReturn(alice);

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
        given(userService.getById(99L))
            .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mvc.perform(get("/api/users/99"))
            .andExpect(status().isNotFound());
    }

    // ── POST /api/users ─────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void createUser_returns201AndBody() throws Exception {
        given(userService.create("Alice", "alice@school.edu", 1L)).willReturn(alice);

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
    @WithMockUser
    void createUser_blankName_returns400() throws Exception {
        String body = mapper.writeValueAsString(
            Map.of("name", "", "email", "alice@school.edu", "schoolIdentifier", 1));

        mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("name"));

        verify(userService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser
    void createUser_invalidEmail_returns400() throws Exception {
        String body = mapper.writeValueAsString(
            Map.of("name", "Alice", "email", "not-an-email", "schoolIdentifier", 1));

        mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("email"));

        verify(userService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser
    void createUser_nullSchoolIdentifier_returns400() throws Exception {
        String body = mapper.writeValueAsString(
            Map.of("name", "Alice", "email", "alice@school.edu"));

        mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("schoolIdentifier"));

        verify(userService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser
    void createUser_duplicateEmail_returns409() throws Exception {
        given(userService.create("Alice", "alice@school.edu", 1L))
            .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use"));

        String body = mapper.writeValueAsString(
            Map.of("name", "Alice", "email", "alice@school.edu", "schoolIdentifier", 1));

        mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
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

    // ── PUT /api/users/{id} ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void updateUser_found_returnsUpdated() throws Exception {
        User updated = User.builder()
            .id(1L).name("Alice Smith").email("alice@school.edu").schoolIdentifier(1L).build();

        given(userService.update(1L, "Alice Smith", "alice@school.edu", 1L)).willReturn(updated);

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
        given(userService.update(eq(99L), any(), any(), any()))
            .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String body = mapper.writeValueAsString(
            Map.of("name", "Ghost", "email", "ghost@x.com", "schoolIdentifier", 999));

        mvc.perform(put("/api/users/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateUser_invalidBody_returns400() throws Exception {
        String body = mapper.writeValueAsString(
            Map.of("name", "", "email", "not-valid", "schoolIdentifier", 1));

        mvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors").isArray());

        verify(userService, never()).update(any(), any(), any(), any());
    }

    // ── DELETE /api/users/{id} ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_asAdmin_returns204() throws Exception {
        mvc.perform(delete("/api/users/1"))
            .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_asUser_returns403() throws Exception {
        mvc.perform(delete("/api/users/1"))
            .andExpect(status().isForbidden());

        verify(userService, never()).delete(any());
    }

    @Test
    void deleteUser_unauthenticated_returns401() throws Exception {
        mvc.perform(delete("/api/users/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_notFound_returns404() throws Exception {
        willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
            .given(userService).delete(99L);

        mvc.perform(delete("/api/users/99"))
            .andExpect(status().isNotFound());

        verify(userService).delete(99L);
    }
}
