# P0 Spring Boot Agent Control Plane Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the M0 trust kernel into a recoverable, auditable Agent control plane with durable state, server-owned evidence assessment, controlled Spring AI Alibaba workflow, resumable SSE, and A2UI human review.

**Architecture:** Keep the modular monolith and its provider-neutral ports. Domain and application modules remain free of JPA and model SDKs; PostgreSQL/H2, DashScope, SSE, and A2UI are adapters wired only by `apps/server`. The 2C2G host runs one bounded control-plane process while external PostgreSQL and DashScope hold durable/model workloads.

**Tech Stack:** JDK 21, Spring Boot 3.5.16, Spring AI Alibaba 1.1.2.2, DashScope, Spring Data JPA, Flyway, PostgreSQL, H2, SSE, A2UI v0.9.1, JUnit 5, MockMvc.

## Global Constraints

- Agent proposals are untrusted; only the server can create `PatchAssessment`, `PolicyDecision`, and mergeable `ResumePatch` records.
- Unsupported facts, numbers, percentages, dates, costs, and metrics never become actionable patches.
- Human acceptance and optimistic base-version validation are mandatory before merge.
- `local` uses file-backed H2 without Docker; `test` uses in-memory H2 and recorded models; `prod` uses external PostgreSQL and DashScope.
- P0 excludes RocketMQ, A2A, PDF/DOCX parsing, sandbox execution, and the render worker.

---

### Task 1: State Machine and Durable Event Contracts

**Produces:** expanded `ResumeTask`, explicit transition commands, `TaskEvent`, event/checkpoint ports, retry/cancel/repair rules.

- [ ] Write failing task-runtime tests for every legal and illegal transition.
- [ ] Add `ANALYZING`, `PROPOSING`, `VERIFYING`, `REVIEW_READY`, and `APPROVED`; remove generic `RUNNING` and `REVIEW_REQUIRED` behavior.
- [ ] Add attempt, repairCount, retryability, traceId, lease metadata, and optimistic revision to the task aggregate.
- [ ] Add immutable task event/checkpoint types and repository ports.
- [ ] Run task-runtime and downstream compile tests, then commit.

### Task 2: H2/PostgreSQL Persistence and Transactional Merge

**Produces:** Flyway schema, JPA-only adapter entities/repositories, local/test/prod profiles, transaction facade.

- [ ] Write repository contract tests for immutable Resume versions, task revisions, patches, evidence, events, checkpoints, and idempotency.
- [ ] Add migrations for `resume_task`, `resume_version`, `resume_patch`, `evidence_artifact`, `task_event`, `workflow_checkpoint`, and `idempotency_record`.
- [ ] Implement JPA adapters without leaking entities outside the adapter package.
- [ ] Wrap merge in a transaction that rechecks policy and atomically writes v2, task completion, and events.
- [ ] Prove rollback and optimistic-lock conflict behavior, then commit.

### Task 3: Server-Owned Evidence Guard

**Produces:** evidence artifacts, claim-level assessments, deterministic pre-checks, guarded submit/edit/merge.

- [ ] Write failing tests for forged coverage, missing/unapproved/expired evidence, unsupported numeric claims, ambiguity, and edited-patch bypass.
- [ ] Add `EvidenceGuard` and `EvidenceArtifactStore` ports plus claim verdict types.
- [ ] Compute coverage from server verdicts; never deserialize coverage or new claims from public requests.
- [ ] Remove public `PatchAssessmentRequest`; allow manual proposal injection only in `local-demo`.
- [ ] Re-run guard during submit, edit, and merge; commit after API/security tests pass.

### Task 4: Controlled Spring AI Alibaba Workflow

**Produces:** structured JD analyst, rewrite, and evidence nodes behind `ResumeAgentWorkflow`.

- [ ] Write recording-model workflow tests for success, timeout, invalid JSON, one repair, and second rejection.
- [ ] Add versioned DTO schemas and prompts for JD capability matrix, proposals, and claim assessments.
- [ ] Build a fixed Graph: load → analyze → propose → pre-check → guard → optional one repair → review-ready.
- [ ] Add redacted `ResumeModelView`, bounded calls/tokens/timeouts, and nullable authoritative cost metadata.
- [ ] Wire `local`, `test`, `ai`, and `prod` adapters; commit only after fake/recording tests are deterministic.

### Task 5: Resumable SSE, Cancellation, and Retry

**Produces:** persistent ordered task stream and command endpoints.

- [ ] Write tests for event order, `Last-Event-ID`, heartbeat, invalid cursor, cancellation, retry, and restart replay.
- [ ] Persist event state in the same transaction as each state change and broadcast only after commit.
- [ ] Add bounded workflow dispatcher with two executions, queue size 20, leases, and checkpoint recovery.
- [ ] Add `/events`, `/cancel`, and `/retry`; redact PII/prompts/secrets from event payloads.
- [ ] Add connection limits and slow-client disconnect/replay behavior, then commit.

### Task 6: ReviewSurface and A2UI v0.9.1

**Produces:** protocol-neutral review model, JSON endpoint, deterministic A2UI adapter, typed review actions.

- [ ] Write tests for patch/evidence/gap/timeline mapping and content negotiation.
- [ ] Add `ReviewSurface`, `PatchReviewItem`, `CoverageGap`, `TaskTimelineItem`, and `AllowedAction`.
- [ ] Generate only whitelisted A2UI components from a versioned custom catalog; reject HTML, scripts, URLs, unknown actions, oversize payloads, and excessive depth.
- [ ] Make EDIT carry a replacement value, rerun the Evidence Guard, and create a patch revision.
- [ ] Verify JSON and `application/a2ui+json` output, then commit.

### Task 7: Full Verification and Handoff

- [ ] Run all Java unit/integration/architecture tests and package the executable jar.
- [ ] Run shared Vue/Java contract tests and the Vue production build.
- [ ] Run the real HTTP flow through create → SSE → review → decision/edit → merge → completed.
- [ ] Verify the unsupported `50%` metric scenario never creates an actionable patch.
- [ ] Update architecture, API, threat-model, profile, and managed-deployment documentation.
