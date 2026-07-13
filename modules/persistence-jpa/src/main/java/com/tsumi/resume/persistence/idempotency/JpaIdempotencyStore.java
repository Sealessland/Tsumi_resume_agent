package com.tsumi.resume.persistence.idempotency;

import com.tsumi.resume.task.IdempotencyConflictException;
import com.tsumi.resume.task.IdempotencyRecord;
import com.tsumi.resume.task.IdempotencyStore;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaIdempotencyStore implements IdempotencyStore {
    private final IdempotencyJpaRepository repository;
    public JpaIdempotencyStore(IdempotencyJpaRepository repository) { this.repository = repository; }
    @Override @Transactional
    public IdempotencyRecord save(IdempotencyRecord record) {
        var id = new IdempotencyId(record.scope(), record.key());
        var existing = repository.findById(id).map(IdempotencyEntity::toDomain);
        if (existing.isPresent()) {
            if (!existing.orElseThrow().requestHash().equals(record.requestHash())) {
                throw new IdempotencyConflictException(record.scope(), record.key());
            }
            return existing.orElseThrow();
        }
        repository.saveAndFlush(new IdempotencyEntity(record));
        return record;
    }
    @Override @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> find(String scope, String key) {
        return repository.findById(new IdempotencyId(scope, key)).map(IdempotencyEntity::toDomain);
    }
}
