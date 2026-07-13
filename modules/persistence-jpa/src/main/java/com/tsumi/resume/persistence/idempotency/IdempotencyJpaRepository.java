package com.tsumi.resume.persistence.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyEntity, IdempotencyId> {}
