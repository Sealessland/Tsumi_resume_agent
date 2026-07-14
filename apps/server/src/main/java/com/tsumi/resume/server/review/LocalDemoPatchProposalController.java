package com.tsumi.resume.server.review;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local-demo", "test"})
@RequestMapping("/api/v1/tasks/{taskId}/patch-proposals")
public class LocalDemoPatchProposalController {
    private final ResumeReviewService reviewService;
    private final JsonContractValidator proposalValidator;
    private final ObjectMapper objectMapper;
    public LocalDemoPatchProposalController(
            ResumeReviewService reviewService,
            @Qualifier("patchProposalContractValidator") JsonContractValidator proposalValidator,
            ObjectMapper objectMapper) {
        this.reviewService = reviewService; this.proposalValidator = proposalValidator; this.objectMapper = objectMapper;
    }
    @PostMapping
    ResponseEntity<ResumePatch> submit(
            @PathVariable("taskId") String taskId,
            @Valid @RequestBody SubmitPatchProposalRequest request) {
        var validation = proposalValidator.validate(request.proposal());
        if (!validation.valid()) throw new ContractRejectedException("resume-patch-proposal", validation.errors());
        var evaluation = reviewService.submit(taskId, toProposal(request));
        var patch = evaluation.patch().orElseThrow(() -> new PolicyRejectedException(evaluation.violations()));
        return ResponseEntity.created(URI.create("/api/v1/tasks/%s/patches/%s".formatted(taskId, patch.patchId())))
                .body(patch);
    }
    private PatchProposal toProposal(SubmitPatchProposalRequest request) {
        try { return objectMapper.treeToValue(request.proposal(), PatchProposal.class); }
        catch (JsonProcessingException exception) {
            throw new ContractRejectedException("resume-patch-proposal",
                    List.of("Payload cannot be mapped to PatchProposal: " + exception.getOriginalMessage()));
        }
    }
}
