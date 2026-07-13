package com.tsumi.resume.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JsonContractValidatorTest {

    private final Path contracts = Path.of(System.getProperty("contracts.dir"));
    private final JsonContractValidator validator =
            new JsonContractValidator(contracts.resolve("resume.schema.json"));

    @Test
    void acceptsSharedValidResumeFixture() {
        var result = validator.validate(
                contracts.resolve("fixtures/resume/valid-minimal-v13.json"));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsSharedFixtureWithoutVersion() {
        var result = validator.validate(
                contracts.resolve("fixtures/resume/invalid-missing-version.json"));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(message -> message.contains("version"));
    }
}
