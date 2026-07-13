# M0B Evidence Policy and Patch Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a deterministic Java trust kernel that separates model proposals from server-owned policy decisions and can apply, revert, and version-check evidence-backed resume patches.

**Architecture:** `PatchProposal` is untrusted Agent output. An independently produced `PatchAssessment` is evaluated by `PatchPolicy`; only an ALLOW result can create a server-owned `ResumePatch` in PENDING review state. `ResumePatchEngine` operates on Resume AST v13 using stable entity IDs, requires human ACCEPTED status, compares the `before` value, increments immutable versions, and supports revert as a new version.

**Tech Stack:** JDK 21, Jackson tree model, JSON Schema draft 2020-12, Ajv 8, JUnit 5, AssertJ

---

## Task 1: Separate Untrusted Patch Proposal from Guarded ResumePatch

**Files:**

- Create: `contracts/resume-patch-proposal.schema.json`
- Create: `contracts/fixtures/patch/valid-paraphrase-proposal.json`
- Create: `contracts/fixtures/patch/invalid-missing-evidence-proposal.json`
- Modify: `contracts/fixtures/resume/valid-minimal-v13.json`
- Modify: `contracts/fixtures/patch/valid-paraphrase.json`
- Modify: `contracts/fixtures/patch/invalid-new-fact.json`
- Modify: `apps/web/src/modules/resume/contracts.js`
- Modify: `apps/web/src/modules/resume/contracts.test.js`
- Modify: `modules/infrastructure/src/test/java/com/tsumi/resume/infrastructure/contract/JsonContractValidatorTest.java`

`PatchProposal` excludes `evidenceCoverage`, `newAtomicClaims`, `riskFlags`, `policyDecision`, and `reviewStatus`; those values are server/reviewer-owned. Change the shared path to `/projects/project_01/description` and include the matching description in the Resume fixture.

- [ ] Add frontend and Java tests for the proposal schema and observe RED.
- [ ] Add the schema/fixtures and validators.
- [ ] Run Vue and Java shared contract tests.
- [ ] Commit as `feat: separate patch proposals from policy decisions`.

## Task 2: Implement Evidence Policy Guard

**Files:**

- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/policy/PatchProposal.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/policy/PatchAssessment.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/policy/PolicyViolation.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/policy/PolicyEvaluation.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/policy/PatchPolicy.java`
- Create: `modules/resume-domain/src/test/java/com/tsumi/resume/domain/policy/PatchPolicyTest.java`
- Modify: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/ResumePatch.java`

Policy violations are `MISSING_EVIDENCE`, `INCOMPLETE_EVIDENCE_COVERAGE`, `UNSUPPORTED_ATOMIC_CLAIM`, `REMOVE_INTENT_MISMATCH`, and `REMOVE_AFTER_NOT_EMPTY`. Rejection returns no mergeable patch. ALLOW creates a `ResumePatch` with server-owned `policyDecision=ALLOW` and `reviewStatus=PENDING`.

- [ ] Write allow/reject tests and observe RED.
- [ ] Implement the minimal policy types and immutable review transition.
- [ ] Run all resume-domain tests.
- [ ] Commit as `feat: enforce evidence-first patch policy`.

## Task 3: Implement Stable-ID Patch Apply, Revert, and Version Conflicts

**Files:**

- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/merge/ResumePatchEngine.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/merge/VersionConflictException.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/merge/PatchConflictException.java`
- Create: `modules/resume-domain/src/test/java/com/tsumi/resume/domain/merge/ResumePatchEngineTest.java`

The resolver treats object tokens as field names and array tokens as stable entity IDs. Apply requires matching `resumeId`, `baseVersion`, `policyDecision=ALLOW`, `reviewStatus=ACCEPTED`, and exact `before` text. Apply increments version by one. Revert verifies the applied `after` value, restores `before`, and increments to a new version rather than mutating history.

- [ ] Write apply/revert/stale-version/before-mismatch/review-required tests and observe RED.
- [ ] Implement the Jackson tree engine with JSON Pointer escaping.
- [ ] Run all resume-domain tests.
- [ ] Commit as `feat: add versioned resume patch engine`.

## Task 4: Complete the Task State after Human Merge

**Files:**

- Modify: `modules/task-runtime/src/main/java/com/tsumi/resume/task/ResumeTask.java`
- Modify: `modules/task-runtime/src/test/java/com/tsumi/resume/task/ResumeTaskTest.java`
- Modify: `docs/architecture/java-modules.md`

Add `complete(Instant)` only from `REVIEW_REQUIRED`. Keep merge execution outside controllers and adapters.

- [ ] Write the failing transition test.
- [ ] Implement the transition and run the full Java reactor.
- [ ] Update architecture documentation and run both verification scripts.
- [ ] Commit as `feat: complete tasks only after human review`.

## Completion Gate

```bash
./scripts/verify-contracts.sh
./scripts/verify-java.sh
git status --short
```

M0B is complete only when model proposals cannot set policy/review fields, unsupported claims produce no ResumePatch, only accepted patches can modify a Resume AST, stale versions and mismatched before-values fail, revert creates a new version, and both runtimes validate the same fixtures.
