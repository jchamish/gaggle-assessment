package com.gaggle.demo.service;

import com.gaggle.demo.entity.Document;
import com.gaggle.demo.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DocumentSaveService {

    private final DocumentRepository documentRepository;

    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public Document save(Document doc) {
        return documentRepository.save(doc);
    }

    @Recover
    public Document recoverSave(ObjectOptimisticLockingFailureException ex, Document doc) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Document was modified concurrently. Fetch the latest version and retry.");
    }
}
