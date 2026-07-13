package com.tsumi.resume.persistence.transaction;

import com.tsumi.resume.workflow.UnitOfWork;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class JpaUnitOfWork implements UnitOfWork {
    private final TransactionTemplate transactions;
    public JpaUnitOfWork(TransactionTemplate transactions) { this.transactions = transactions; }
    @Override public <T> T execute(Supplier<T> work) {
        return transactions.execute(status -> work.get());
    }
}
