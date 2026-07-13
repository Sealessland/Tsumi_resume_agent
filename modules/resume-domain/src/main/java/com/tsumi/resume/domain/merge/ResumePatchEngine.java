package com.tsumi.resume.domain.merge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.domain.patch.PatchOperation;
import com.tsumi.resume.domain.patch.PolicyDecision;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.patch.ReviewStatus;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public final class ResumePatchEngine {

    public ObjectNode apply(ObjectNode source, ResumePatch patch) {
        return applyAll(source, List.of(patch));
    }

    public ObjectNode applyAll(ObjectNode source, List<ResumePatch> patches) {
        if (patches.isEmpty()) {
            throw new PatchConflictException("Patch merge requires at least one patch");
        }
        var first = patches.getFirst();
        requireMergeable(first);
        requireIdentityAndVersion(source, first, first.baseVersion());
        var copy = source.deepCopy();
        var paths = new HashSet<String>();
        for (var patch : patches) {
            requireMergeable(patch);
            if (!patch.resumeId().equals(first.resumeId())) {
                throw new PatchConflictException("All patches must target the same resume");
            }
            if (patch.baseVersion() != first.baseVersion()) {
                throw new VersionConflictException(first.baseVersion(), patch.baseVersion());
            }
            if (!paths.add(patch.path())) {
                throw new PatchConflictException(
                        "Patch batch contains duplicate path: " + patch.path());
            }
            applyContent(copy, patch);
        }
        copy.put("version", first.baseVersion() + 1);
        return copy;
    }

    private void applyContent(ObjectNode copy, ResumePatch patch) {
        var target = resolveTarget(copy, patch.path());
        var current = target.parent().get(target.field());
        requireTextValue(current, patch.path());
        if (!current.asText().equals(patch.before())) {
            throw new PatchConflictException(
                    "Patch before value does not match current resume at " + patch.path());
        }

        if (patch.op() == PatchOperation.REMOVE) {
            target.parent().remove(target.field());
        } else {
            target.parent().put(target.field(), patch.after());
        }
    }

    public ObjectNode revert(ObjectNode currentResume, ResumePatch appliedPatch) {
        requireMergeable(appliedPatch);
        requireIdentityAndVersion(
                currentResume, appliedPatch, appliedPatch.baseVersion() + 1);
        var copy = currentResume.deepCopy();
        var target = resolveTarget(copy, appliedPatch.path());
        var current = target.parent().get(target.field());

        if (appliedPatch.op() == PatchOperation.REMOVE) {
            if (current != null) {
                throw new PatchConflictException(
                        "Cannot revert remove because the target exists at " + appliedPatch.path());
            }
            target.parent().put(target.field(), appliedPatch.before());
        } else {
            requireTextValue(current, appliedPatch.path());
            if (!current.asText().equals(appliedPatch.after())) {
                throw new PatchConflictException(
                        "Cannot revert because the applied value changed at " + appliedPatch.path());
            }
            target.parent().put(target.field(), appliedPatch.before());
        }
        copy.put("version", appliedPatch.baseVersion() + 2);
        return copy;
    }

    private void requireMergeable(ResumePatch patch) {
        if (patch.policyDecision() != PolicyDecision.ALLOW) {
            throw new PatchConflictException("Patch policy decision must be ALLOW");
        }
        if (patch.reviewStatus() != ReviewStatus.ACCEPTED) {
            throw new PatchConflictException("Patch review status must be ACCEPTED");
        }
        if (patch.evidenceRefs().isEmpty()
                || Double.compare(patch.evidenceCoverage(), 1.0) != 0
                || !patch.newAtomicClaims().isEmpty()) {
            throw new PatchConflictException("Patch does not satisfy evidence merge requirements");
        }
        var tokens = pathTokens(patch.path());
        var leaf = tokens.getLast();
        if (leaf.equals("id")
                || (tokens.size() == 1
                && (leaf.equals("resumeId")
                || leaf.equals("version")
                || leaf.equals("schemaVersion")))) {
            throw new PatchConflictException("Patch path targets a protected identity field");
        }
    }

    private void requireIdentityAndVersion(
            ObjectNode resume, ResumePatch patch, long expectedVersion) {
        if (!patch.resumeId().equals(resume.path("resumeId").asText())) {
            throw new PatchConflictException("Patch resumeId does not match target resume");
        }
        if (resume.path("schemaVersion").asInt() != 13) {
            throw new PatchConflictException("Patch engine requires Resume AST schemaVersion 13");
        }
        var actualVersion = resume.path("version").asLong();
        if (actualVersion != expectedVersion) {
            throw new VersionConflictException(expectedVersion, actualVersion);
        }
    }

    private Target resolveTarget(ObjectNode root, String path) {
        var tokens = pathTokens(path);
        JsonNode current = root;
        for (int index = 0; index < tokens.size() - 1; index++) {
            var token = tokens.get(index);
            if (current instanceof ObjectNode object) {
                current = object.get(token);
                if (current == null) {
                    throw new PatchConflictException("Path field does not exist: " + token);
                }
            } else if (current instanceof ArrayNode array) {
                current = findEntity(array, token);
            } else {
                throw new PatchConflictException("Path cannot traverse token: " + token);
            }
        }
        if (!(current instanceof ObjectNode parent)) {
            throw new PatchConflictException("Patch target parent is not an object: " + path);
        }
        return new Target(parent, tokens.getLast());
    }

    private JsonNode findEntity(ArrayNode array, String entityId) {
        for (var candidate : array) {
            if (candidate instanceof ObjectNode object
                    && entityId.equals(object.path("id").asText())) {
                return object;
            }
        }
        throw new PatchConflictException("Stable entity id does not exist: " + entityId);
    }

    private List<String> pathTokens(String path) {
        if (path == null || !path.startsWith("/") || path.length() == 1) {
            throw new PatchConflictException("Patch path must be a non-root JSON pointer");
        }
        var tokens = Arrays.stream(path.substring(1).split("/", -1))
                .map(this::decodePointerToken)
                .toList();
        if (tokens.stream().anyMatch(String::isEmpty)) {
            throw new PatchConflictException("Patch path contains an empty token");
        }
        return tokens;
    }

    private String decodePointerToken(String token) {
        return token.replace("~1", "/").replace("~0", "~");
    }

    private void requireTextValue(JsonNode value, String path) {
        if (value == null || !value.isTextual()) {
            throw new PatchConflictException("Patch target is not an existing text value: " + path);
        }
    }

    private record Target(ObjectNode parent, String field) {}
}
