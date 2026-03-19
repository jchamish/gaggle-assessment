package com.gaggle.demo.service;

import com.gaggle.demo.entity.Document;
import com.gaggle.demo.entity.User;
import com.gaggle.demo.repository.DocumentRepository;
import com.gaggle.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentSaveService documentSaveService;

    public Page<Document> listAll(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    public Document getById(Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    public Document create(String title, String content, Long createdById) {
        User creator = resolveUser(createdById);
        Document doc = Document.builder()
            .title(title)
            .content(content)
            .createdBy(creator)
            .lastEditedBy(creator)
            .build();
        return documentSaveService.save(doc);
    }

    public Document update(Long id, String title, String content, Long lastEditedById) {
        Document doc = getById(id);
        User editor = resolveUser(lastEditedById);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setLastEditedBy(editor);
        return documentSaveService.save(doc);
    }

    public void delete(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        documentRepository.deleteById(id);
    }

    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found: " + userId));
    }
}
