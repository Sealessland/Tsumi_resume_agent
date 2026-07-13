package com.tsumi.resume.infrastructure.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

public final class JsonContractValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchema schema;

    public JsonContractValidator(Path schemaPath) {
        this.objectMapper = new ObjectMapper();
        this.schema = JsonSchemaFactory
                .getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(read(schemaPath));
    }

    public JsonContractValidator(InputStream schemaInput) {
        this.objectMapper = new ObjectMapper();
        this.schema = JsonSchemaFactory
                .getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(read(schemaInput));
    }

    public ContractValidationResult validate(Path documentPath) {
        return validate(read(documentPath));
    }

    public ContractValidationResult validate(JsonNode document) {
        var messages = schema.validate(document);
        List<String> errors = messages.stream()
                .map(Object::toString)
                .sorted()
                .toList();
        return new ContractValidationResult(errors.isEmpty(), errors);
    }

    private JsonNode read(Path path) {
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read JSON contract artifact: " + path, exception);
        }
    }

    private JsonNode read(InputStream input) {
        try (input) {
            return objectMapper.readTree(input);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read packaged JSON schema", exception);
        }
    }
}
