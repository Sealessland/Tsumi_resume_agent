# M0C Human Review and Merge API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the runnable Spring Boot skeleton into a complete local review-and-merge backend vertical slice with immutable resume versions, server-owned Patch policy, human decisions, and merge APIs.

**Architecture:** Ports and use cases live in `agent-workflow`; in-memory adapters live in `infrastructure`; REST DTOs and error mapping stay in `apps/server`. Resume versions are immutable Jackson trees. A merge reads the expected base version, applies all accepted patches as one new version, persists it, and completes the task only after review.

**Tech Stack:** JDK 21, Spring Boot 3.5.16, Jackson, Jakarta Validation, JUnit 5, MockMvc

---

## Task 1: Add Immutable Resume and Patch Store Ports

- Create `ResumeVersionStore`, `PatchStore`, and `VersionedResumeService` in `modules/agent-workflow`.
- Create focused tests proving save-by-version, duplicate-version rejection, and defensive JSON copies.
- Implement in-memory adapters in `modules/infrastructure`.
- Commit as `feat: add immutable resume version stores`.

## Task 2: Add Batch Patch Merge Semantics

- Extend `ResumePatchEngine` with `applyAll(ObjectNode, List<ResumePatch>)`.
- Test two accepted patches merge into one new version, duplicate paths fail, and all patches require the same base version.
- Keep single apply/revert behavior unchanged.
- Commit as `feat: merge accepted patches as one resume version`.

## Task 3: Add Review and Merge Application Service

- Create `ResumeReviewService` in `modules/agent-workflow`.
- Submit receives `PatchProposal` plus independently supplied `PatchAssessment`; rejected evaluations never enter `PatchStore`.
- Decision accepts only `ACCEPTED`, `REJECTED`, or `EDITED` and checks task/patch/base version.
- Merge loads accepted patches, calls `applyAll`, saves the new resume version, and moves the task to COMPLETED.
- Test rejection, acceptance, stale merge, and successful immutable merge.
- Commit as `feat: orchestrate human patch review and merge`.

## Task 4: Expose Resume, Proposal, Decision, and Merge APIs

Minimum local API:

```text
POST /api/v1/resumes
GET  /api/v1/resumes/{resumeId}/versions
GET  /api/v1/resumes/{resumeId}/versions/{version}
POST /api/v1/tasks/{taskId}/patch-proposals
POST /api/v1/tasks/{taskId}/patches/{patchId}/decision
POST /api/v1/tasks/{taskId}/merge
```

- Add request/response DTOs with Jakarta Validation.
- Add stable RFC 7807 codes for contract rejection, policy rejection, version conflict, patch conflict, and missing resume/patch.
- Extend Spring Boot integration tests through import → task → proposal → accept → merge → version query.
- Commit as `feat: expose evidence review and merge api`.

## Task 5: Verify and Document the Vertical Slice

- Update README and `docs/architecture/java-modules.md` with curl examples and local limitations.
- Run frontend contract tests/build and the full Maven reactor.
- Start the executable jar and run the vertical slice over HTTP.
- Commit as `docs: document local review merge workflow`.
