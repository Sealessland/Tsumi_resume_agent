package com.tsumi.resume.server.review;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import com.tsumi.resume.server.api.HttpIdempotencyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}")
public class PatchReviewController {

    private final ResumeReviewService reviewService;
    private final HttpIdempotencyService idempotency;
    public PatchReviewController(
            ResumeReviewService reviewService,
            HttpIdempotencyService idempotency) {
        this.reviewService = reviewService;
        this.idempotency = idempotency;
    }

    @GetMapping("/patches")
    List<ResumePatch> patches(@PathVariable("taskId") String taskId) {
        return reviewService.patches(taskId);
    }

    @PostMapping("/patches/{patchId}/decision")
    ResumePatch decide(
            @PathVariable("taskId") String taskId,
            @PathVariable("patchId") String patchId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReviewDecisionRequest request) {
        return idempotency.execute(
                "patch:decision:" + taskId + ":" + patchId,
                idempotencyKey,
                request,
                200,
                ResumePatch.class,
                () -> reviewService.decide(
                        taskId, patchId, request.expectedBaseVersion(), request.decision()));
    }

    @PostMapping("/merge")
    ResponseEntity<ObjectNode> merge(
            @PathVariable("taskId") String taskId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody MergePatchesRequest request) {
        var merged = idempotency.execute(
                "patch:merge:" + taskId,
                idempotencyKey,
                request,
                201,
                ObjectNode.class,
                () -> reviewService.merge(taskId, request.expectedBaseVersion()));
        var location = URI.create("/api/v1/resumes/%s/versions/%d".formatted(
                merged.path("resumeId").asText(), merged.path("version").asLong()));
        return ResponseEntity.created(location).body(merged);
    }

}
