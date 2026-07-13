package com.tsumi.resume.persistence.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record IdempotencyId(
        @Column(name = "scope_name", length = 160) String scope,
        @Column(name = "idempotency_key", length = 160) String key) implements Serializable {
    protected IdempotencyId() { this(null, null); }
}
