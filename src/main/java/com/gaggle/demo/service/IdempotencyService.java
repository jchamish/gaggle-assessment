package com.gaggle.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaggle.demo.entity.IdempotencyRecord;
import com.gaggle.demo.repository.IdempotencyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class IdempotencyService {

    static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyRepository repository;
    // Owned here rather than injected — ObjectMapper is not a primary bean in Boot 4.x,
    // and findAndRegisterModules() picks up JavaTimeModule for LocalDateTime support.
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public IdempotencyService(IdempotencyRepository repository) {
        this.repository = repository;
    }

    public <T> Optional<T> find(String key, Class<T> responseType) {
        return repository.findById(key)
            .filter(r -> r.getCreatedAt().isAfter(LocalDateTime.now().minus(TTL)))
            .map(r -> {
                try {
                    return objectMapper.readValue(r.getResponseBody(), responseType);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Failed to deserialize idempotency record for key: " + key, e);
                }
            });
    }

    public <T> void store(String key, T response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            repository.save(new IdempotencyRecord(key, json, LocalDateTime.now()));
        } catch (JsonProcessingException e) {
            // A failed store doesn't affect the primary response — the next request
            // with this key will be treated as a new request
            log.warn("Failed to store idempotency record for key {}: {}", key, e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3_600_000) // hourly
    @Transactional
    public void purgeExpired() {
        repository.deleteByCreatedAtBefore(LocalDateTime.now().minus(TTL));
    }
}
