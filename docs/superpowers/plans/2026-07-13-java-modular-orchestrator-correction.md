# Java Modular Orchestrator Correction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the contract-only Java placeholder with a locally runnable modular-monolith backend that creates and queries resume rewrite tasks through a deterministic fake Agent workflow.

**Architecture:** Keep `resume-domain` framework-light, place task state and repository ports in `task-runtime`, place orchestration use cases and the Agent port in `agent-workflow`, put JSON Schema and in-memory/fake implementations in `infrastructure`, and expose them only through `apps/server`. The dependency direction is `server -> infrastructure + agent-workflow -> task-runtime + resume-domain`; provider adapters never leak into domain modules.

**Tech Stack:** JDK 21, Maven reactor, Spring Boot 3.5.16, Jakarta Validation, Jackson, NetworkNT JSON Schema Validator, JUnit 5, AssertJ

---

## Scope Boundaries

This correction deliberately does not connect a paid model, PostgreSQL, RocketMQ, A2UI, A2A, or a document sandbox. It creates the stable executable seam those adapters will plug into. The local fake workflow is deterministic and clearly labeled; it never invents resume content.

## Dependency Graph

```text
apps/server
  ├── modules/agent-workflow
  └── modules/infrastructure
          ├── modules/agent-workflow
          ├── modules/task-runtime
          └── modules/resume-domain

modules/agent-workflow
  ├── modules/task-runtime
  └── modules/resume-domain

modules/task-runtime
  └── modules/resume-domain
```

## Task 1: Extract Contract Infrastructure from the Domain

**Files:**

- Create: `modules/infrastructure/pom.xml`
- Move: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/contract/*` → `modules/infrastructure/src/main/java/com/tsumi/resume/infrastructure/contract/`
- Move: `modules/resume-domain/src/test/java/com/tsumi/resume/domain/contract/*` → `modules/infrastructure/src/test/java/com/tsumi/resume/infrastructure/contract/`
- Modify: `modules/resume-domain/src/test/java/com/tsumi/resume/domain/patch/ResumePatchDeserializationTest.java`
- Modify: root `pom.xml`
- Modify: `modules/resume-domain/pom.xml`

- [ ] Add a Maven architecture assertion that `resume-domain` does not resolve Spring or NetworkNT artifacts.
- [ ] Run it and observe RED while NetworkNT still belongs to `resume-domain`.
- [ ] Move JSON Schema validation classes and dependencies to `infrastructure`; update imports and the reactor.
- [ ] Run both module tests and confirm the shared fixtures still pass.
- [ ] Commit as `refactor: separate contract infrastructure from domain`.

## Task 2: Introduce the Task Runtime Module

**Files:**

- Create: `modules/task-runtime/pom.xml`
- Create: `modules/task-runtime/src/main/java/com/tsumi/resume/task/TaskStatus.java`
- Create: `modules/task-runtime/src/main/java/com/tsumi/resume/task/ResumeTask.java`
- Create: `modules/task-runtime/src/main/java/com/tsumi/resume/task/TaskRepository.java`
- Create: `modules/task-runtime/src/main/java/com/tsumi/resume/task/TaskNotFoundException.java`
- Create: `modules/task-runtime/src/test/java/com/tsumi/resume/task/ResumeTaskTest.java`

`ResumeTask` exposes explicit transitions only:

```java
public ResumeTask start(Instant now);
public ResumeTask requireReview(String workflowSummary, Instant now);
public ResumeTask fail(String failureCode, Instant now);
```

Allowed MVP path is `CREATED -> RUNNING -> REVIEW_REQUIRED`; terminal states cannot transition. `TaskRepository` contains only `save(ResumeTask)` and `findById(String)`.

- [ ] Write tests for the allowed path and illegal terminal transitions.
- [ ] Run them and observe RED because the module/types do not exist.
- [ ] Implement immutable state transitions and the repository port.
- [ ] Run the module tests and commit as `feat: add explicit resume task runtime`.

## Task 3: Introduce the Agent Workflow Application Module

**Files:**

- Create: `modules/agent-workflow/pom.xml`
- Create: `modules/agent-workflow/src/main/java/com/tsumi/resume/workflow/ResumeAgentWorkflow.java`
- Create: `modules/agent-workflow/src/main/java/com/tsumi/resume/workflow/WorkflowInput.java`
- Create: `modules/agent-workflow/src/main/java/com/tsumi/resume/workflow/WorkflowResult.java`
- Create: `modules/agent-workflow/src/main/java/com/tsumi/resume/workflow/CreateTaskCommand.java`
- Create: `modules/agent-workflow/src/main/java/com/tsumi/resume/workflow/TaskOrchestrator.java`
- Create: `modules/agent-workflow/src/test/java/com/tsumi/resume/workflow/TaskOrchestratorTest.java`

The Agent boundary is typed and provider-neutral:

```java
public interface ResumeAgentWorkflow {
    WorkflowResult execute(WorkflowInput input);
}
```

`TaskOrchestrator` receives `TaskRepository`, `ResumeAgentWorkflow`, `Clock`, and `Supplier<String>` through its constructor. `create()` persists CREATED, then RUNNING, executes the workflow, and persists REVIEW_REQUIRED. `get()` throws `TaskNotFoundException` for unknown IDs.

- [ ] Write tests with a recording fake to prove state persistence order and typed workflow input.
- [ ] Run them and observe RED because application types do not exist.
- [ ] Implement only the synchronous local orchestration path.
- [ ] Run all non-Spring module tests and commit as `feat: add provider-neutral agent orchestration`.

## Task 4: Add Infrastructure Adapters and a Runnable Spring Boot Server

**Files:**

- Create: `modules/infrastructure/src/main/java/com/tsumi/resume/infrastructure/task/InMemoryTaskRepository.java`
- Create: `modules/infrastructure/src/main/java/com/tsumi/resume/infrastructure/workflow/LocalDeterministicWorkflow.java`
- Create: `apps/server/pom.xml`
- Create: `apps/server/src/main/java/com/tsumi/resume/server/TsumiResumeServerApplication.java`
- Create: `apps/server/src/main/java/com/tsumi/resume/server/config/LocalRuntimeConfiguration.java`
- Create: `apps/server/src/main/java/com/tsumi/resume/server/task/CreateTaskRequest.java`
- Create: `apps/server/src/main/java/com/tsumi/resume/server/task/TaskResponse.java`
- Create: `apps/server/src/main/java/com/tsumi/resume/server/task/TaskController.java`
- Create: `apps/server/src/main/java/com/tsumi/resume/server/api/ApiExceptionHandler.java`
- Create: `apps/server/src/main/resources/application.yml`
- Create: `apps/server/src/test/java/com/tsumi/resume/server/task/TaskApiIntegrationTest.java`

Minimum API:

```text
POST /api/v1/tasks          -> 201 + Location
GET  /api/v1/tasks/{taskId} -> 200
GET  /actuator/health       -> 200
```

`POST` accepts `resumeId`, `baseVersion`, and non-blank `jobDescription`. The local workflow returns only a diagnostic summary such as `LOCAL_FAKE_READY_FOR_REVIEW`; it emits no resume claims or Patch content.

- [ ] Write Spring Boot integration tests for create/get/validation/not-found.
- [ ] Run them and observe RED because the server does not exist.
- [ ] Add Spring Boot 3.5.16 dependency management, web/validation/actuator starters, configuration beans, controller, and RFC 7807 error mapping.
- [ ] Run server integration tests and package an executable jar.
- [ ] Start the jar on a random local port and verify `/actuator/health` responds `UP`.
- [ ] Commit as `feat: add runnable modular orchestrator api`.

## Task 5: Replace the Misleading Verification and Documentation

**Files:**

- Modify: `scripts/verify-contracts.sh`
- Create: `scripts/verify-java.sh`
- Modify: `README.md`
- Create: `docs/architecture/java-modules.md`

- [ ] Add `verify-java.sh` to run the full Maven reactor and server integration tests.
- [ ] Keep `verify-contracts.sh` focused on shared Vue/Java fixture compatibility while pointing Java validation at `infrastructure`.
- [ ] Document the dependency graph, local fake boundary, local start command, and the future Spring AI Alibaba adapter seam.
- [ ] Run both verification scripts and `git diff --check`.
- [ ] Commit as `docs: document runnable Java module boundaries`.

## Completion Gate

Run with JDK 21 and Node >=22.12:

```bash
./scripts/verify-contracts.sh
./scripts/verify-java.sh
git status --short
```

Expected: frontend five contract tests pass, Vite builds, every Maven module test passes, the Spring Boot integration test passes, the executable server jar is produced, and the worktree is clean after commits.
