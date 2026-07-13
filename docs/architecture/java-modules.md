# Java 模块边界

Java 后端是一个模块化单体，不是把 Controller、Agent、数据库和消息队列堆进同一个 Spring Boot module。`apps/server` 是唯一可部署进程，其余模块都是可独立测试的库。

```text
apps/server
  ├── agent-workflow
  └── infrastructure
          ├── agent-workflow
          ├── task-runtime
          └── resume-domain

agent-workflow -> task-runtime
```

## 模块职责

| 模块 | 负责 | 明确不负责 |
| --- | --- | --- |
| `resume-domain` | ResumePatch、意图、操作、审核状态 | Spring、NetworkNT、Controller、模型 SDK |
| `task-runtime` | ResumeTask 聚合、显式状态转换、TaskRepository 端口 | HTTP、数据库实现、Agent SDK |
| `agent-workflow` | TaskOrchestrator、ResumeAgentWorkflow 端口、类型化输入输出 | DashScope 配置、存储实现、Web DTO |
| `infrastructure` | JSON Schema、内存仓储、本地确定性 workflow | REST API、业务状态跳转 |
| `apps/server` | Spring Boot 启动、Bean 装配、REST/ProblemDetail、Actuator | 领域规则、模型推理细节 |

## 当前本地纵切

`POST /api/v1/tasks` 经过 Jakarta Validation 后进入 `TaskOrchestrator`。Orchestrator 按顺序持久化 `CREATED`、`RUNNING`、`REVIEW_REQUIRED`，并通过 `ResumeAgentWorkflow` 调用当前 adapter。`LocalDeterministicWorkflow` 只返回 `LOCAL_FAKE_READY_FOR_REVIEW`，不会生成 Patch 或新增简历事实。任务只有在 `REVIEW_REQUIRED` 后才能转为 `COMPLETED`，Agent 与 Controller 都不能绕过人工审核状态。

模型输出使用独立的 `PatchProposal` 合同，不包含 `policyDecision` 或 `reviewStatus`。`PatchPolicy` 根据独立 `PatchAssessment` 生成服务器拥有的 `ResumePatch`；`ResumePatchEngine` 只合并 `ALLOW + ACCEPTED`、证据覆盖为 100%、不存在新增原子事实、版本和 before 值均匹配的 Patch。apply 和 revert 都创建新版本，稳定实体 ID 属于受保护字段。

`GET /api/v1/tasks/{taskId}` 返回当前任务；未知任务统一返回带稳定 `TASK_NOT_FOUND` code 的 RFC 7807 响应。`/actuator/health` 是当前唯一暴露的 Actuator endpoint。

## Spring AI Alibaba 接入位置

根 BOM 固定 Spring AI Alibaba `1.1.2.2` 与 Spring Boot `3.5.16` 兼容线。真实实现应新增 provider adapter 实现 `ResumeAgentWorkflow`，通过 profile 或配置替换 `LocalDeterministicWorkflow`。它只能返回类型化 workflow 结果，不能直接操作 Controller、TaskRepository 或前端 A2UI。

后续接入顺序：

1. 在 `agent-workflow` 定义 evidence/review 节点的结构化输入输出。
2. 在 `infrastructure` 增加 Spring AI Alibaba Graph adapter。
3. 通过固定 fake-model recording 测试工作流成功、超时、Policy 拒绝和人工中断。
4. 再增加 PostgreSQL、RocketMQ LiteTopic、A2UI 与 sandbox adapters。

## 验证

```bash
./scripts/verify-contracts.sh
./scripts/verify-java.sh
```

`verify-contracts.sh` 证明 Vue 与 Java 使用同一份 JSON Schema/fixture；`verify-java.sh` 对整个 Maven reactor 执行 clean verify，包括 server 集成测试与可执行 jar 打包。
