package com.tsumi.resume.server.review;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.workflow.review.ReviewSurface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class A2uiReviewSurfaceAdapter {
    public static final String MEDIA_TYPE = "application/a2ui+json";
    public static final String VERSION = "v0.9.1";
    public static final String CATALOG_ID = "urn:tsumi:a2ui:resume-review:0.9.1";
    private static final int MAX_COMPONENTS = 100;
    private static final int MAX_PAYLOAD_BYTES = 256 * 1024;
    private static final int MAX_DEPTH = 8;
    private static final Set<String> COMPONENTS = Set.of(
            "DiffCard", "EvidenceList", "RiskBadge", "CoverageGap",
            "ReviewActions", "TaskTimeline", "CostSummary");

    private final ObjectMapper objectMapper;

    public A2uiReviewSurfaceAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toMessageStream(ReviewSurface surface) {
        var messages = List.of(
                createSurface(surface),
                updateComponents(surface),
                updateDataModel(surface));
        var stream = messages.stream().map(this::write).collect(java.util.stream.Collectors.joining("\n")) + "\n";
        if (stream.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            throw new A2uiPayloadLimitException("A2UI payload exceeds 256KB");
        }
        return stream;
    }

    private ObjectNode createSurface(ReviewSurface surface) {
        var message = envelope();
        var create = message.putObject("createSurface");
        create.put("surfaceId", surface.surfaceId());
        create.put("catalogId", CATALOG_ID);
        create.put("sendDataModel", false);
        return message;
    }

    private ObjectNode updateComponents(ReviewSurface surface) {
        var message = envelope();
        var update = message.putObject("updateComponents");
        update.put("surfaceId", surface.surfaceId());
        var components = update.putArray("components");
        var childIds = new ArrayList<String>();

        for (int index = 0; index < surface.patches().size(); index++) {
            var patchId = "patch_" + index;
            childIds.add(patchId);
            add(components, patchId, "DiffCard", "/patches/" + index, List.of(
                    "patch_" + index + "_evidence", "patch_" + index + "_risks"));
            add(components, "patch_" + index + "_evidence", "EvidenceList",
                    "/patches/" + index + "/evidence", List.of());
            add(components, "patch_" + index + "_risks", "RiskBadge",
                    "/patches/" + index + "/risks", List.of());
        }
        for (int index = 0; index < surface.gaps().size(); index++) {
            var id = "gap_" + index;
            childIds.add(id);
            add(components, id, "CoverageGap", "/gaps/" + index, List.of());
        }
        childIds.add("timeline");
        childIds.add("cost_summary");
        add(components, "timeline", "TaskTimeline", "/timeline", List.of());
        add(components, "cost_summary", "CostSummary", "/costSummary", List.of());

        var root = component("root", "ReviewActions", "/actions", childIds);
        components.insert(0, root);
        if (components.size() > MAX_COMPONENTS) {
            throw new A2uiPayloadLimitException("A2UI component count exceeds 100");
        }
        validateComponentDepth(components);
        return message;
    }

    private ObjectNode updateDataModel(ReviewSurface surface) {
        var message = envelope();
        var update = message.putObject("updateDataModel");
        update.put("surfaceId", surface.surfaceId());
        update.put("path", "/");
        update.set("value", objectMapper.valueToTree(surface));
        return message;
    }

    private void add(
            ArrayNode components,
            String id,
            String component,
            String dataPath,
            List<String> children) {
        components.add(component(id, component, dataPath, children));
    }

    private ObjectNode component(
            String id,
            String component,
            String dataPath,
            List<String> children) {
        if (!COMPONENTS.contains(component)) {
            throw new IllegalArgumentException("Unregistered A2UI component: " + component);
        }
        var node = objectMapper.createObjectNode();
        node.put("id", id);
        node.put("component", component);
        node.put("dataPath", dataPath);
        if (!children.isEmpty()) {
            var values = node.putArray("children");
            children.forEach(values::add);
        }
        return node;
    }

    private ObjectNode envelope() {
        return objectMapper.createObjectNode().put("version", VERSION);
    }

    private String write(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("A2UI message cannot be serialized", exception);
        }
    }

    private void validateComponentDepth(ArrayNode components) {
        Map<String, List<String>> childrenById = new HashMap<>();
        components.forEach(component -> {
            var children = new ArrayList<String>();
            component.path("children").forEach(child -> children.add(child.asText()));
            childrenById.put(component.path("id").asText(), List.copyOf(children));
        });
        if (componentDepth("root", childrenById, new HashSet<>()) > MAX_DEPTH) {
            throw new A2uiPayloadLimitException("A2UI component nesting exceeds 8 levels");
        }
    }

    private int componentDepth(
            String id,
            Map<String, List<String>> childrenById,
            Set<String> visiting) {
        if (!visiting.add(id)) {
            throw new A2uiPayloadLimitException("A2UI component graph contains a cycle");
        }
        int max = 0;
        for (var child : childrenById.getOrDefault(id, List.of())) {
            if (!childrenById.containsKey(child)) {
                throw new A2uiPayloadLimitException("A2UI component references an unknown child");
            }
            max = Math.max(max, componentDepth(child, childrenById, visiting));
        }
        visiting.remove(id);
        return max + 1;
    }
}
