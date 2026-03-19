package com.gaggle.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String responseBody;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
