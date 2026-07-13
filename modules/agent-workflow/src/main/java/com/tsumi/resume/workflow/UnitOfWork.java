package com.tsumi.resume.workflow;

import java.util.function.Supplier;

@FunctionalInterface
public interface UnitOfWork {
    <T> T execute(Supplier<T> work);

    static UnitOfWork direct() {
        return new UnitOfWork() {
            @Override public <T> T execute(Supplier<T> work) { return work.get(); }
        };
    }
}
