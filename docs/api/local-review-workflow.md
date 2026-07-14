# Evidence-first Agent Review API

后端采用模块化单体：任务异步执行固定 Graph，服务端 Evidence Guard 决定 Patch 是否可进入人工审核，浏览器和 Rewrite Agent 都不能提交 coverage 或 policyDecision。

## 本地启动

```bash
mvn -pl apps/server -am package
java -jar apps/server/target/server-0.1.0-SNAPSHOT.jar
```

默认 `local` profile 使用文件型 H2（`./.data/tsumi-resume`）和确定性模型录制；`ai` 使用本地 H2 + DashScope；`prod` 使用外部 PostgreSQL + DashScope。生产密钥只从环境变量注入。

## 主流程

1. `POST /api/v1/resumes` 导入不可变 Resume v1。
2. `POST /api/v1/tasks` 创建异步任务，必须携带 `Idempotency-Key`，返回 `202 CREATED` 和 `eventsUrl`。
3. `GET /api/v1/tasks/{taskId}/events` 订阅 SSE；支持 `Last-Event-ID` 补发。
4. 固定 Graph 执行 JD Analyst → Rewrite → deterministic pre-check → Evidence Guard → 最多一次修复。
5. `GET /api/v1/tasks/{taskId}/review-surface` 获取可视化 Diff、Evidence、claim verdict、风险、Gap 和时间线。
6. 使用 `ACCEPT / REJECT / EDIT` 人工审核；所有 mutation 必须携带 `Idempotency-Key`。
7. `POST /api/v1/tasks/{taskId}/merge` 原子写入 Resume v2、APPROVED/COMPLETED 状态和事件。

创建任务示例：

```bash
curl -i -X POST http://localhost:8080/api/v1/tasks \
  -H 'Idempotency-Key: create-demo-01' \
  -H 'Content-Type: application/json' \
  -d '{"resumeId":"res_fixture","baseVersion":1,"jobDescription":"Java Agent Engineer"}'
```

## Review Surface 与 A2UI

普通 JSON：

```bash
curl -H 'Accept: application/json' \
  "http://localhost:8080/api/v1/tasks/$TASK_ID/review-surface"
```

A2UI v0.9.1 JSONL 消息流：

```bash
curl -H 'Accept: application/a2ui+json' \
  "http://localhost:8080/api/v1/tasks/$TASK_ID/review-surface"
```

A2UI 由确定性 Adapter 生成，catalog 固定为 `urn:tsumi:a2ui:resume-review:0.9.1`。仅允许 DiffCard、EvidenceList、RiskBadge、CoverageGap、ReviewActions、TaskTimeline、CostSummary；最多 100 个组件、256KB、8 层组件树。模型不能直接生成组件、HTML、JavaScript 或 action。

人工编辑：

```bash
curl -X POST \
  "http://localhost:8080/api/v1/tasks/$TASK_ID/patches/$PATCH_ID/edit" \
  -H 'Idempotency-Key: edit-demo-01' \
  -H 'Content-Type: application/json' \
  -d '{"expectedBaseVersion":1,"after":"人工修改后的内容"}'
```

EDIT 会重新运行服务端 Evidence Guard。只有所有 claim 都为 `SUPPORTED` 才创建新的 `_r2` Patch；原 Patch 标记为 `EDITED`。拒绝时不产生 revision，也不会显示可接受的占位 Patch。

## 信任与数据约束

- `/patch-proposals` 仅在 `local-demo/test` profile 开放，且 Assessment 始终由服务端计算。
- 新数字、百分比、日期、金额和专有名词若不在 before 或批准 Evidence 中，Patch 不落库。
- 未获得权威价格时 `estimatedCost=null`；禁止推测 ATS、成本、性能或业务指标。
- SSE payload 不包含简历全文、Prompt、密钥或签名 URL。
- 模型调用前默认移除姓名、电话、邮箱、照片和地址。

## 关键错误码

`IDEMPOTENCY_CONFLICT`、`VERSION_CONFLICT`、`TASK_NOT_RETRYABLE`、`TASK_CANCELLED`、`WORKFLOW_TIMEOUT`、`MODEL_OUTPUT_REJECTED`、`EVIDENCE_NOT_APPROVED`、`SSE_CURSOR_INVALID`、`POLICY_REJECTED`。

所有错误使用 RFC 7807。意外异常仅返回 traceId，不泄漏模型输出、SQL、PII 或凭据。
