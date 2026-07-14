package com.tsumi.resume.server.review;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsumi.resume.domain.patch.ResumePatch;
import com.tsumi.resume.workflow.review.ResumeReviewService;
import com.tsumi.resume.workflow.review.ReviewSurface;
import com.tsumi.resume.workflow.review.ReviewSurfaceService;
import com.tsumi.resume.server.api.HttpIdempotencyService;
import com.tsumi.resume.server.api.PolicyRejectedException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
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
    private final ReviewSurfaceService surfaces;
    private final A2uiReviewSurfaceAdapter a2ui;
    public PatchReviewController(
            ResumeReviewService reviewService,
            HttpIdempotencyService idempotency,
            ReviewSurfaceService surfaces,
            A2uiReviewSurfaceAdapter a2ui) {
        this.reviewService = reviewService;
        this.idempotency = idempotency;
        this.surfaces = surfaces;
        this.a2ui = a2ui;
    }

    @GetMapping("/patches")
    List<ResumePatch> patches(@PathVariable("taskId") String taskId) {
        return reviewService.patches(taskId);
    }

    @GetMapping(value = "/review-surface", produces = MediaType.APPLICATION_JSON_VALUE)
    ReviewSurface reviewSurface(@PathVariable("taskId") String taskId) {
        return surfaces.get(taskId);
    }

    @GetMapping(value = "/review-surface", produces = A2uiReviewSurfaceAdapter.MEDIA_TYPE)
    ResponseEntity<String> a2uiReviewSurface(@PathVariable("taskId") String taskId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(A2uiReviewSurfaceAdapter.MEDIA_TYPE))
                .body(a2ui.toMessageStream(surfaces.get(taskId)));
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

    @PostMapping("/patches/{patchId}/edit")
    ResumePatch edit(
            @PathVariable("taskId") String taskId,
            @PathVariable("patchId") String patchId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody EditPatchRequest request) {
        return idempotency.execute(
                "patch:edit:" + taskId + ":" + patchId,
                idempotencyKey,
                request,
                200,
                ResumePatch.class,
                () -> {
                    var evaluation = reviewService.edit(
                            taskId, patchId, request.expectedBaseVersion(), request.after());
                    return evaluation.patch().orElseThrow(
                            () -> new PolicyRejectedException(evaluation.violations()));
                });
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
