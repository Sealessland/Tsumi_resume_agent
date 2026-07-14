package com.tsumi.resume.ai.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Set;

public final class ResumeModelSanitizer {

    private static final Set<String> PII_FIELDS = Set.of(
            "name", "fullname", "email", "phone", "telephone", "mobile",
            "photo", "avatar", "portrait", "address", "locationaddress");

    public ResumeModelView sanitize(ObjectNode resume) {
        var copy = resume.deepCopy();
        removePii(copy);
        return new ResumeModelView(
                resume.path("resumeId").asText(), resume.path("version").asLong(), copy);
    }

    private void removePii(JsonNode node) {
        if (node instanceof ObjectNode object) {
            var fields = new java.util.ArrayList<String>();
            object.fieldNames().forEachRemaining(fields::add);
            for (var field : fields) {
                if (PII_FIELDS.contains(normalize(field))) object.remove(field);
                else removePii(object.get(field));
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(this::removePii);
        }
    }

    private String normalize(String field) {
        return field.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }
}
