package com.gaggle.demo.controller;

import com.gaggle.demo.dto.PageResponse;
import com.gaggle.demo.entity.Document;
import com.gaggle.demo.service.DocumentService;
import com.gaggle.demo.service.IdempotencyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final IdempotencyService idempotencyService;

    @GetMapping
    public PageResponse<Document> listDocuments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(documentService.listAll(pageable));
    }

    @GetMapping("/{id}")
    public Document getDocument(@PathVariable Long id) {
        return documentService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Document> createDocument(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DocumentRequest req) {

        if (idempotencyKey != null) {
            Optional<Document> cached = idempotencyService.find(idempotencyKey, Document.class);
            if (cached.isPresent()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(cached.get());
            }
        }

        Document saved = documentService.create(req.title(), req.content(), req.createdById());

        if (idempotencyKey != null) {
            idempotencyService.store(idempotencyKey, saved);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Document updateDocument(@PathVariable Long id, @Valid @RequestBody DocumentUpdateRequest req) {
        return documentService.update(id, req.title(), req.content(), req.lastEditedById());
    }

    // ADMIN only — enforced in SecurityConfig
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    record DocumentRequest(
        @NotBlank String title,
        String content,
        @NotNull Long createdById
    ) {}

    record DocumentUpdateRequest(
        @NotBlank String title,
        String content,
        @NotNull Long lastEditedById
    ) {}
}
