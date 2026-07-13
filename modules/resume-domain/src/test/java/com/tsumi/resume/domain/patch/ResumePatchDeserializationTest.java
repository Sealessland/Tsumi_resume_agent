package com.tsumi.resume.domain.patch;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ResumePatchDeserializationTest {

    private final Path contracts = Path.of(System.getProperty("contracts.dir"));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesSharedParaphraseFixture() throws Exception {
        ResumePatch patch = objectMapper.readValue(
                contracts.resolve("fixtures/patch/valid-paraphrase.json").toFile(),
                ResumePatch.class);

        assertThat(patch.intent()).isEqualTo(PatchIntent.PARAPHRASE);
        assertThat(patch.evidenceRefs()).containsExactly("resume:projects/project_01");
        assertThat(patch.newAtomicClaims()).isEmpty();
    }
}
