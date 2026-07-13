package com.tsumi.resume.domain.contract;

import java.util.List;

public record ContractValidationResult(boolean valid, List<String> errors) {

    public ContractValidationResult {
        errors = List.copyOf(errors);
    }
}
