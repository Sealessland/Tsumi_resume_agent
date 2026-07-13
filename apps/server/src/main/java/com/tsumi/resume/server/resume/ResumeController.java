package com.tsumi.resume.server.resume;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.infrastructure.contract.JsonContractValidator;
import com.tsumi.resume.server.api.ContractRejectedException;
import com.tsumi.resume.workflow.resume.VersionedResumeService;
import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final VersionedResumeService resumeService;
    private final JsonContractValidator contractValidator;

    public ResumeController(
            VersionedResumeService resumeService,
            @Qualifier("resumeContractValidator") JsonContractValidator contractValidator) {
        this.resumeService = resumeService;
        this.contractValidator = contractValidator;
    }

    @PostMapping
    ResponseEntity<ObjectNode> importVersion(@RequestBody ObjectNode resume) {
        var validation = contractValidator.validate(resume);
        if (!validation.valid()) {
            throw new ContractRejectedException("resume", validation.errors());
        }
        var saved = resumeService.register(resume);
        var location = URI.create("/api/v1/resumes/%s/versions/%d".formatted(
                saved.path("resumeId").asText(), saved.path("version").asLong()));
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping("/{resumeId}/versions")
    ResumeVersionsResponse versions(@PathVariable("resumeId") String resumeId) {
        return new ResumeVersionsResponse(resumeId, resumeService.versions(resumeId));
    }

    @GetMapping("/{resumeId}/versions/{version}")
    ObjectNode version(
            @PathVariable("resumeId") String resumeId,
            @PathVariable("version") long version) {
        return resumeService.get(resumeId, version);
    }
}
