package com.tsumi.resume.domain.patch;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PatchOperation {
    @JsonProperty("replace") REPLACE,
    @JsonProperty("remove") REMOVE
}
