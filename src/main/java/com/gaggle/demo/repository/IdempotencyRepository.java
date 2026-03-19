package com.gaggle.demo.repository;

import com.gaggle.demo.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
