package com.tsumi.resume.domain.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;

public final class ResumePathResolver {
    public JsonNode resolve(ObjectNode root, String path) {
        JsonNode current = root;
        for (var token : tokens(path)) {
            if (current instanceof ObjectNode object) {
                current = object.get(token);
                if (current == null) throw new PatchConflictException("Path field does not exist: " + token);
            } else if (current instanceof ArrayNode array) {
                current = findById(array, token);
            } else {
                throw new PatchConflictException("Path cannot traverse token: " + token);
            }
        }
        return current;
    }

    private JsonNode findById(ArrayNode array, String id) {
        for (var candidate : array) {
            if (candidate instanceof ObjectNode object && id.equals(object.path("id").asText())) return object;
        }
        throw new PatchConflictException("Stable entity id does not exist: " + id);
    }

    private List<String> tokens(String path) {
        if (path == null || !path.startsWith("/") || path.length() == 1) {
            throw new PatchConflictException("Patch path must be a non-root JSON pointer");
        }
        var tokens = Arrays.stream(path.substring(1).split("/", -1))
                .map(token -> token.replace("~1", "/").replace("~0", "~")).toList();
        if (tokens.stream().anyMatch(String::isEmpty)) {
            throw new PatchConflictException("Patch path contains an empty token");
        }
        return tokens;
    }
}
