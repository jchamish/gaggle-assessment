package com.gaggle.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaggle.demo.config.SecurityConfig;
import com.gaggle.demo.entity.Document;
import com.gaggle.demo.entity.User;
import com.gaggle.demo.repository.DocumentRepository;
import com.gaggle.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentControllerTest {

    @Autowired MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();
    @MockitoBean DocumentRepository documentRepository;
    @MockitoBean UserRepository userRepository;

    private final User alice = User.builder()
        .id(1L).name("Alice").email("alice@school.edu").schoolIdentifier(1L).build();

    private final Document essay = Document.builder()
        .id(10L).title("My Essay").content("Once upon a time...")
        .createdBy(alice).lastEditedBy(alice).build();

    // ── GET /api/documents ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    void listDocuments_returnsAll() throws Exception {
        given(documentRepository.findAll()).willReturn(List.of(essay));

        mvc.perform(get("/api/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("My Essay"));
    }

    @Test
    @WithMockUser
    void listDocuments_empty_returnsEmptyArray() throws Exception {
        given(documentRepository.findAll()).willReturn(List.of());

        mvc.perform(get("/api/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listDocuments_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/documents"))
            .andExpect(status().isUnauthorized());
    }

    // ── GET /api/documents/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void getDocument_found_returnsDocument() throws Exception {
        given(documentRepository.findById(10L)).willReturn(Optional.of(essay));

        mvc.perform(get("/api/documents/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("My Essay"))
            .andExpect(jsonPath("$.content").value("Once upon a time..."))
            .andExpect(jsonPath("$.createdBy.name").value("Alice"));
    }

    @Test
    @WithMockUser
    void getDocument_notFound_returns404() throws Exception {
        given(documentRepository.findById(99L)).willReturn(Optional.empty());

        mvc.perform(get("/api/documents/99"))
            .andExpect(status().isNotFound());
    }

    // ── POST /api/documents ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void createDocument_validUser_returns201() throws Exception {
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(documentRepository.save(any())).willReturn(essay);

        String body = mapper.writeValueAsString(
            Map.of("title", "My Essay", "content", "Once upon a time...", "createdById", 1));

        mvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("My Essay"))
            .andExpect(jsonPath("$.createdBy.email").value("alice@school.edu"));
    }

    @Test
    @WithMockUser
    void createDocument_unknownUser_returns400() throws Exception {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        String body = mapper.writeValueAsString(
            Map.of("title", "My Essay", "content", "Content", "createdById", 999));

        mvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createDocument_unauthenticated_returns401() throws Exception {
        String body = mapper.writeValueAsString(
            Map.of("title", "My Essay", "content", "Content", "createdById", 1));

        mvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/documents/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void updateDocument_found_returnsUpdated() throws Exception {
        Document updated = Document.builder()
            .id(10L).title("Revised Essay").content("Better content")
            .createdBy(alice).lastEditedBy(alice).build();

        given(documentRepository.findById(10L)).willReturn(Optional.of(essay));
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(documentRepository.save(any())).willReturn(updated);

        String body = mapper.writeValueAsString(
            Map.of("title", "Revised Essay", "content", "Better content", "lastEditedById", 1));

        mvc.perform(put("/api/documents/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Revised Essay"));
    }

    @Test
    @WithMockUser
    void updateDocument_docNotFound_returns404() throws Exception {
        given(documentRepository.findById(99L)).willReturn(Optional.empty());

        String body = mapper.writeValueAsString(
            Map.of("title", "X", "content", "Y", "lastEditedById", 1));

        mvc.perform(put("/api/documents/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateDocument_unknownEditor_returns400() throws Exception {
        given(documentRepository.findById(10L)).willReturn(Optional.of(essay));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        String body = mapper.writeValueAsString(
            Map.of("title", "X", "content", "Y", "lastEditedById", 999));

        mvc.perform(put("/api/documents/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/documents/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDocument_asAdmin_returns204() throws Exception {
        given(documentRepository.existsById(10L)).willReturn(true);

        mvc.perform(delete("/api/documents/10"))
            .andExpect(status().isNoContent());

        verify(documentRepository).deleteById(10L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteDocument_asUser_returns403() throws Exception {
        mvc.perform(delete("/api/documents/10"))
            .andExpect(status().isForbidden());

        verify(documentRepository, never()).deleteById(any());
    }

    @Test
    void deleteDocument_unauthenticated_returns401() throws Exception {
        mvc.perform(delete("/api/documents/10"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDocument_notFound_returns404() throws Exception {
        given(documentRepository.existsById(99L)).willReturn(false);

        mvc.perform(delete("/api/documents/99"))
            .andExpect(status().isNotFound());

        verify(documentRepository, never()).deleteById(any());
    }
}