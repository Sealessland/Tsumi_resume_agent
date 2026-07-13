package com.tsumi.resume.infrastructure.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JsonContractValidatorTest {

    private final Path contracts = Path.of(System.getProperty("contracts.dir"));

    @Test
    void acceptsSharedValidResumeFixture() {
        var validator = new JsonContractValidator(contracts.resolve("resume.schema.json"));
        var result = validator.validate(
                contracts.resolve("fixtures/resume/valid-minimal-v13.json"));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsSharedFixtureWithoutVersion() {
        var validator = new JsonContractValidator(contracts.resolve("resume.schema.json"));
        var result = validator.validate(
                contracts.resolve("fixtures/resume/invalid-missing-version.json"));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(message -> message.contains("version"));
    }

    @Test
    void rejectsSharedPatchContainingUnsupportedClaim() {
        var validator = new JsonContractValidator(contracts.resolve("resume-patch.schema.json"));

        assertThat(validator.validate(
                contracts.resolve("fixtures/patch/invalid-new-fact.json")).valid()).isFalse();
    }

    @Test
    void acceptsSharedModelProposalWithoutServerOwnedDecisionFields() {
        var validator = new JsonContractValidator(
                contracts.resolve("resume-patch-proposal.schema.json"));

        assertThat(validator.validate(
                contracts.resolve("fixtures/patch/valid-paraphrase-proposal.json")).valid())
                .isTrue();
    }

    @Test
    void rejectsSharedModelProposalWithoutEvidenceReferences() {
        var validator = new JsonContractValidator(
                contracts.resolve("resume-patch-proposal.schema.json"));

        assertThat(validator.validate(
                contracts.resolve("fixtures/patch/invalid-missing-evidence-proposal.json")).valid())
                .isFalse();
    }
}
