package com.gaggle.demo.controller;

import com.gaggle.demo.entity.Document;
import com.gaggle.demo.entity.User;
import com.gaggle.demo.repository.DocumentRepository;
import com.gaggle.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<Document> listDocuments() {
        return documentRepository.findAll();
    }

    @GetMapping("/{id}")
    public Document getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@RequestBody DocumentRequest req) {
        User creator = resolveUser(req.createdById());
        Document doc = Document.builder()
            .title(req.title())
            .content(req.content())
            .createdBy(creator)
            .lastEditedBy(creator)
            .build();
        return documentRepository.save(doc);
    }

    @PutMapping("/{id}")
    public Document updateDocument(@PathVariable Long id, @RequestBody DocumentUpdateRequest req) {
        Document doc = documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        User editor = resolveUser(req.lastEditedById());
        doc.setTitle(req.title());
        doc.setContent(req.content());
        doc.setLastEditedBy(editor);
        return documentRepository.save(doc);
    }

    // ADMIN only — enforced in SecurityConfig
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        if (!documentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        documentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found: " + userId));
    }

    record DocumentRequest(String title, String content, Long createdById) {}
    record DocumentUpdateRequest(String title, String content, Long lastEditedById) {}
}