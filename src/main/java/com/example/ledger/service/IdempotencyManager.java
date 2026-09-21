package com.example.ledger.service;

import com.example.ledger.model.IdempotencyKey;
import com.example.ledger.model.IdempotencyStatus;
import com.example.ledger.repository.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyManager {

    private final IdempotencyKeyRepository repository;

    public IdempotencyManager(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyKey initiateOrRecover(String key, String requestHash) {
        return repository.findById(key).map(existing -> {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException("Idempotency key reused with different payload");
            }
            return existing;
        }).orElseGet(() -> {
            IdempotencyKey newKey = new IdempotencyKey();
            newKey.setKey(key);
            newKey.setRequestHash(requestHash);
            newKey.setStatus(IdempotencyStatus.PROCESSING);
            repository.save(newKey);
            return null; // <--- This tells the filter: "This is brand new, proceed!"
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, int statusCode, String responseBody, boolean isSuccess) {
        IdempotencyKey existing = repository.findById(key)
                .orElseThrow(() -> new IllegalStateException("Key not found"));

        existing.setStatus(isSuccess ? IdempotencyStatus.SUCCESS : IdempotencyStatus.FAILED);
        existing.setResponseCode(statusCode);
        existing.setResponseBody(responseBody);
        repository.save(existing);
    }
}