package com.tsumi.resume.server.review;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.infrastructure.contract.JsonContractValidator;
import com.tsumi.resume.server.api.ContractRejectedException;
import com.tsumi.resume.server.api.PolicyRejectedException;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}")
public class PatchReviewController {

    private final ResumeReviewService reviewService;
    private final JsonContractValidator proposalValidator;
    private final ObjectMapper objectMapper;

    public PatchReviewController(
            ResumeReviewService reviewService,
            @Qualifier("patchProposalContractValidator") JsonContractValidator proposalValidator,
            ObjectMapper objectMapper) {
        this.reviewService = reviewService;
        this.proposalValidator = proposalValidator;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/patch-proposals")
    ResponseEntity<ResumePatch> submit(
            @PathVariable("taskId") String taskId,
            @Valid @RequestBody SubmitPatchProposalRequest request) {
        var validation = proposalValidator.validate(request.proposal());
        if (!validation.valid()) {
            throw new ContractRejectedException("resume-patch-proposal", validation.errors());
        }

        var proposal = toProposal(request);
        var evaluation = reviewService.submit(taskId, proposal, request.assessment().toDomain());
        var patch = evaluation.patch()
                .orElseThrow(() -> new PolicyRejectedException(evaluation.violations()));
        var location = URI.create("/api/v1/tasks/%s/patches/%s".formatted(
                taskId, patch.patchId()));
        return ResponseEntity.created(location).body(patch);
    }

    @GetMapping("/patches")
    List<ResumePatch> patches(@PathVariable("taskId") String taskId) {
        return reviewService.patches(taskId);
    }

    @PostMapping("/patches/{patchId}/decision")
    ResumePatch decide(
            @PathVariable("taskId") String taskId,
            @PathVariable("patchId") String patchId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        return reviewService.decide(
                taskId, patchId, request.expectedBaseVersion(), request.decision());
    }

    @PostMapping("/merge")
    ResponseEntity<ObjectNode> merge(
            @PathVariable("taskId") String taskId,
            @Valid @RequestBody MergePatchesRequest request) {
        var merged = reviewService.merge(taskId, request.expectedBaseVersion());
        var location = URI.create("/api/v1/resumes/%s/versions/%d".formatted(
                merged.path("resumeId").asText(), merged.path("version").asLong()));
        return ResponseEntity.created(location).body(merged);
    }

    private PatchProposal toProposal(SubmitPatchProposalRequest request) {
        try {
            return objectMapper.treeToValue(request.proposal(), PatchProposal.class);
        } catch (JsonProcessingException exception) {
            throw new ContractRejectedException(
                    "resume-patch-proposal",
                    List.of("Payload cannot be mapped to PatchProposal: "
                            + exception.getOriginalMessage()));
        }
    }
}
