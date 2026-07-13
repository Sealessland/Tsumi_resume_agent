package com.tsumi.resume.server.api;

import java.util.List;

public final class ContractRejectedException extends RuntimeException {

    private final String contract;
    private final List<String> violations;

    public ContractRejectedException(String contract, List<String> violations) {
        super("Payload does not satisfy the " + contract + " contract");
        this.contract = contract;
        this.violations = List.copyOf(violations);
    }

    public String contract() {
        return contract;
    }

    public List<String> violations() {
        return violations;
    }
}
