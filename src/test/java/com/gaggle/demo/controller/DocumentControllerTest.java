package com.gaggle.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaggle.demo.config.SecurityConfig;
import com.gaggle.demo.entity.Document;
import com.gaggle.demo.entity.User;
import com.gaggle.demo.exception.GlobalExceptionHandler;
import com.gaggle.demo.service.DocumentService;
import com.gaggle.demo.service.IdempotencyService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DocumentControllerTest {

    @Autowired MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();
    @MockitoBean DocumentService documentService;
    @MockitoBean IdempotencyService idempotencyService;

    private final User alice = User.builder()
        .id(1L).name("Alice").email("alice@school.edu").schoolIdentifier(1L).build();

    private final Document essay = Document.builder()
        .id(10L).title("My Essay").content("Once upon a time...")
        .createdBy(alice).lastEditedBy(alice).build();

    // ── GET /api/documents ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    void listDocuments_returnsAll() throws Exception {
        given(documentService.listAll(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(essay)));

        mvc.perform(get("/api/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("My Essay"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser
    void listDocuments_empty_returnsEmptyArray() throws Exception {
        given(documentService.listAll(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser
    void listDocuments_customPage_passesPageableToRepo() throws Exception {
        given(documentService.listAll(any(Pageable.class)))
            .willAnswer(inv -> new PageImpl<>(List.of(essay), inv.getArgument(0), 1));

        mvc.perform(get("/api/documents?page=0&size=5&sort=title,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(5));
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
        given(documentService.getById(10L)).willReturn(essay);

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
        given(documentService.getById(99L))
            .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        mvc.perform(get("/api/documents/99"))
            .andExpect(status().isNotFound());
    }

    // ── POST /api/documents ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void createDocument_validUser_returns201() throws Exception {
        given(documentService.create("My Essay", "Once upon a time...", 1L)).willReturn(essay);
        given(idempotencyService.find(any(), any())).willReturn(Optional.empty());

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
    void createDocument_withIdempotencyKey_newKey_creates() throws Exception {
        given(documentService.create("My Essay", "Once upon a time...", 1L)).willReturn(essay);
        given(idempotencyService.find(eq("key-123"), eq(Document.class))).willReturn(Optional.empty());

        String body = mapper.writeValueAsString(
            Map.of("title", "My Essay", "content", "Once upon a time...", "createdById", 1));

        mvc.perform(post("/api/documents")
                .header("Idempotency-Key", "key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("My Essay"));

        verify(idempotencyService).store(eq("key-123"), any(Document.class));
    }

    @Test
    @WithMockUser
    void createDocument_withIdempotencyKey_duplicateKey_returnsCached() throws Exception {
        given(idempotencyService.find(eq("key-123"), eq(Document.class))).willReturn(Optional.of(essay));

        String body = mapper.writeValueAsString(
            Map.of("title", "My Essay", "content", "Once upon a time...", "createdById", 1));

        mvc.perform(post("/api/documents")
                .header("Idempotency-Key", "key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10));

        verify(documentService, never()).create(any(), any(), any());
        verify(idempotencyService, never()).store(any(), any());
    }

    @Test
    @WithMockUser
    void createDocument_unknownUser_returns400() throws Exception {
        given(documentService.create(any(), any(), eq(999L)))
            .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found: 999"));
        given(idempotencyService.find(any(), any())).willReturn(Optional.empty());

        String body = mapper.writeValueAsString(
            Map.of("title", "My Essay", "content", "Content", "createdById", 999));

        mvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createDocument_blankTitle_returns400() throws Exception {
        String body = mapper.writeValueAsString(
            Map.of("title", "", "content", "Content", "createdById", 1));

        mvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("title"));

        verify(documentService, never()).create(any(), any(), any());
    }

    @Test
    @WithMockUser
    void createDocument_nullCreatedById_returns400() throws Exception {
        String body = mapper.writeValueAsString(
            Map.of("title", "My Essay", "content", "Content"));

        mvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("createdById"));

        verify(documentService, never()).create(any(), any(), any());
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

        given(documentService.update(10L, "Revised Essay", "Better content", 1L)).willReturn(updated);

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
        given(documentService.update(eq(99L), any(), any(), any()))
            .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

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
        given(documentService.update(eq(10L), any(), any(), eq(999L)))
            .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found: 999"));

        String body = mapper.writeValueAsString(
            Map.of("title", "X", "content", "Y", "lastEditedById", 999));

        mvc.perform(put("/api/documents/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void updateDocument_blankTitle_returns400() throws Exception {
        String body = mapper.writeValueAsString(
            Map.of("title", "", "content", "Y", "lastEditedById", 1));

        mvc.perform(put("/api/documents/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("title"));

        verify(documentService, never()).update(any(), any(), any(), any());
    }

    // ── DELETE /api/documents/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDocument_asAdmin_returns204() throws Exception {
        mvc.perform(delete("/api/documents/10"))
            .andExpect(status().isNoContent());

        verify(documentService).delete(10L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteDocument_asUser_returns403() throws Exception {
        mvc.perform(delete("/api/documents/10"))
            .andExpect(status().isForbidden());

        verify(documentService, never()).delete(any());
    }

    @Test
    void deleteDocument_unauthenticated_returns401() throws Exception {
        mvc.perform(delete("/api/documents/10"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDocument_notFound_returns404() throws Exception {
        willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"))
            .given(documentService).delete(99L);

        mvc.perform(delete("/api/documents/99"))
            .andExpect(status().isNotFound());

        verify(documentService).delete(99L);
    }
}
