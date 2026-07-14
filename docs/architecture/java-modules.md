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
| `resume-domain` | PatchProposal、Policy Guard、ResumePatch、Patch Engine | Spring、NetworkNT、Controller、模型 SDK |
| `task-runtime` | ResumeTask 聚合、显式状态转换、TaskRepository 端口 | HTTP、数据库实现、Agent SDK |
| `agent-workflow` | 异步任务用例、ResumeAgentWorkflow 端口、Evidence Review、ReviewSurface | DashScope 配置、存储实现、Web DTO |
| `infrastructure` | JSON Schema validator、内存仓储、本地确定性 workflow | REST API、业务状态跳转 |
| `persistence-jpa` | JPA Entity/Adapter、Flyway、事务 UnitOfWork | 领域规则、Controller |
| `spring-ai-adapter` | 固定 Spring AI Alibaba Graph、DashScope 结构化节点、PII 脱敏 | 自由路由、最终 Policy 决策 |
| `apps/server` | Spring Boot 启动、Bean 装配、REST/ProblemDetail、Actuator | 领域规则、模型推理细节 |

## 当前 P0 纵切

`POST /api/v1/tasks` 返回 `202`。本地执行器以数据库租约恢复任务，最多并行 2 个、排队 20 个；状态固定为 `CREATED → ANALYZING → PROPOSING → VERIFYING → REVIEW_READY → APPROVED → COMPLETED`。Graph 只传结构化 DTO，并在人审前 interrupt。

模型输出使用独立的 `PatchProposal` 合同，不包含 `policyDecision` 或 `reviewStatus`。Proposal 先经过共享 Schema，`PatchPolicy` 再根据独立 `PatchAssessment` 生成服务器拥有的 `ResumePatch`；Policy 拒绝的 Proposal 永远不会写入 `PatchStore`。`ResumePatchEngine` 只合并 `ALLOW + ACCEPTED`、证据覆盖为 100%、不存在新增原子事实、版本和 before 值均匹配的 Patch。批量 merge 只创建一个新版本，稳定实体 ID 属于受保护字段。

任务只有在至少一个 Patch 经人工 `ACCEPTED` 并成功生成新 Resume 版本后才能转为 `COMPLETED`。Controller 不能直接改变任务状态或修改 Resume JSON。未知任务、Schema 拒绝、Policy 拒绝和版本冲突统一返回带稳定 `code` 的 RFC 7807 响应。`/actuator/health` 是当前唯一暴露的 Actuator endpoint。

完整 HTTP 协议和 curl 流程见 [`../api/local-review-workflow.md`](../api/local-review-workflow.md)。

## Spring AI Alibaba 与 Review Surface

根 BOM 固定 Spring AI Alibaba `1.1.2.2` 与 Spring Boot `3.5.16`。`spring-ai-adapter` 已实现固定 Graph；模型不能选择下一阶段，也不能决定 policyDecision。ReviewSurface 是协议无关领域模型，A2UI v0.9.1 由 server 的确定性白名单 Adapter 映射。

P0 明确不包含 RocketMQ、A2A、PDF/DOCX Worker、sandbox 和真实渲染 Worker；这些后续替换调度或工具 Adapter，不改变状态机和 Evidence 信任边界。

## 验证

```bash
./scripts/verify-contracts.sh
./scripts/verify-java.sh
```

`verify-contracts.sh` 证明 Vue 与 Java 使用同一份 JSON Schema/fixture；`verify-java.sh` 对整个 Maven reactor 执行 clean verify，包括 server 集成测试与可执行 jar 打包。
