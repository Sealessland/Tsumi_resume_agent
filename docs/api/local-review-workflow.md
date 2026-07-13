# 本地 Evidence-first Review/Merge API

这条纵向切片用于本地开发、自动化测试和比赛演示：导入不可变 Resume v1，创建 Agent 任务，提交候选 Patch，人工决定，最后原子合并为 v2。默认 workflow 是确定性 fake，不调用模型。

## 启动

```bash
mvn -pl apps/server -am package
java -jar apps/server/target/server-0.1.0-SNAPSHOT.jar
```

## 1. 导入 Resume v1

```bash
curl -i -X POST http://localhost:8080/api/v1/resumes \
  -H 'Content-Type: application/json' \
  --data-binary @contracts/fixtures/resume/valid-minimal-v13.json
```

成功返回 `201`，`Location: /api/v1/resumes/res_fixture/versions/1`。同一 `resumeId + version` 不能覆盖，重复导入返回 `409 DUPLICATE_RESOURCE`。

## 2. 创建任务

```bash
curl -i -X POST http://localhost:8080/api/v1/tasks \
  -H 'Content-Type: application/json' \
  -d '{"resumeId":"res_fixture","baseVersion":1,"jobDescription":"Java Agent Engineer"}'
```

记录响应中的 `taskId`。本地 fake workflow 会让任务进入 `REVIEW_REQUIRED`；不存在的 Resume 版本返回 `404 RESUME_VERSION_NOT_FOUND`。

以下命令用实际值替换 `$TASK_ID`。

## 3. 提交候选 Patch

```bash
curl -i -X POST "http://localhost:8080/api/v1/tasks/$TASK_ID/patch-proposals" \
  -H 'Content-Type: application/json' \
  -d '{
    "proposal": {
      "patchId": "rp_demo",
      "taskId": "'$TASK_ID'",
      "resumeId": "res_fixture",
      "baseVersion": 1,
      "op": "replace",
      "path": "/projects/project_01/description",
      "before": "实现简历编辑和导出功能。",
      "after": "打通结构化编辑、实时预览及 PDF/PNG 导出链路。",
      "intent": "PARAPHRASE",
      "evidenceRefs": ["resume:projects/project_01"],
      "jdRefs": ["jd:delivery/export"],
      "confidence": 0.92
    },
    "assessment": {
      "evidenceCoverage": 1.0,
      "newAtomicClaims": [],
      "riskFlags": []
    }
  }'
```

成功返回服务器拥有的 `ResumePatch`，其初始状态一定是 `policyDecision=ALLOW`、`reviewStatus=PENDING`。如果 `evidenceRefs` 为空，Schema 返回 `422 CONTRACT_REJECTED`；如果 `newAtomicClaims` 非空或覆盖率不足，Policy 返回 `422 POLICY_REJECTED`，并且 Patch 不会落库。

> 本地演示请求显式携带 `assessment`，便于在没有真实模型和 Evidence Guard 时测试完整用例。生产环境不能信任浏览器或外部 Agent 自报的 assessment，必须由受信任的服务器节点生成并保护该写入端点。

## 4. 人工审核

```bash
curl -i -X POST \
  "http://localhost:8080/api/v1/tasks/$TASK_ID/patches/rp_demo/decision" \
  -H 'Content-Type: application/json' \
  -d '{"expectedBaseVersion":1,"decision":"ACCEPTED"}'
```

可用决定为 `ACCEPTED`、`REJECTED`、`EDITED`。决定是终态；重复决定返回 `409 REVIEW_CONFLICT`。`PENDING` 不能作为人工决定。

## 5. 原子合并并查询 v2

```bash
curl -i -X POST "http://localhost:8080/api/v1/tasks/$TASK_ID/merge" \
  -H 'Content-Type: application/json' \
  -d '{"expectedBaseVersion":1}'

curl http://localhost:8080/api/v1/resumes/res_fixture/versions
curl http://localhost:8080/api/v1/resumes/res_fixture/versions/2
curl "http://localhost:8080/api/v1/tasks/$TASK_ID"
```

merge 只读取 `ACCEPTED` Patch，校验版本、before 值、证据覆盖和受保护字段，再一次性生成 v2。成功后任务才会变成 `COMPLETED`。v1 保持不可变。

## 稳定错误码

| HTTP | code | 含义 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Jakarta Validation 失败 |
| 400 | `MALFORMED_JSON` | JSON 无法解析或枚举非法 |
| 404 | `TASK_NOT_FOUND` | 任务不存在 |
| 404 | `RESUME_VERSION_NOT_FOUND` | Resume 版本不存在 |
| 404 | `PATCH_NOT_FOUND` | Patch 不存在 |
| 409 | `DUPLICATE_RESOURCE` | 不可变版本或 Patch 重复 |
| 409 | `VERSION_CONFLICT` | expectedBaseVersion 已过期 |
| 409 | `REVIEW_CONFLICT` | 审核/合并状态冲突 |
| 422 | `CONTRACT_REJECTED` | 共享 JSON Schema 拒绝 |
| 422 | `POLICY_REJECTED` | 证据策略拒绝，Patch 不落库 |

所有错误使用 `application/problem+json`，并包含 `code`、`retryable` 和 `nextAction`。

## 当前本地限制

- Resume、Task、Patch 使用进程内存仓储，重启即清空。
- 未启用认证、限流和租户隔离，不能直接暴露公网。
- `LocalDeterministicWorkflow` 不调用 Spring AI Alibaba 或外部模型。
- 未启用 RocketMQ、PostgreSQL、对象存储或 sandbox。
