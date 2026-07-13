package com.tsumi.resume.domain;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

class DomainDependencyBoundaryTest {

    @Test
    void doesNotExposeInfrastructureLibrariesOnItsClasspath() {
        assertThatExceptionOfType(ClassNotFoundException.class)
                .isThrownBy(() -> Class.forName("com.networknt.schema.JsonSchema"));
        assertThatExceptionOfType(ClassNotFoundException.class)
                .isThrownBy(() -> Class.forName("org.springframework.context.ApplicationContext"));
    }
}
