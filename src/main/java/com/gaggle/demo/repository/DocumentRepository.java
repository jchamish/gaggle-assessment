package com.gaggle.demo.repository;

import com.gaggle.demo.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByCreatedById(Long userId);
}