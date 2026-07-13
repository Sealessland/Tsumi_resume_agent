# Tsumi Resume Studio

一个正在向 Evidence-first Agent 工作台演进的简历项目。前端保留 `Vue 3 + Vite` 的「左侧编辑 + 右侧实时预览」体验，后端采用 JDK 21 + Spring Boot 模块化单体。

现有编辑、预览和导出链路保持可用；Java 侧已经提供可执行 Orchestrator API、本地确定性 Agent 适配器、显式任务状态机、不可变 Resume 版本、人工 Patch Review/Merge 和共享 Resume/ResumePatch 合同。

## 项目定位

- Web 编辑器可独立本地运行；Agent 能力通过独立 Spring Boot API 接入
- Agent 只能生成有证据约束的候选修改，最终合并必须由用户确认
- 默认围绕一页 A4 中文简历布局设计
- 支持校招 / 社招常见模块：教育、技能、实习、项目、奖项、证书、自我评价
- 面向“可打印、可导出、可本地持久化”的技术简历场景

## 当前已实现的功能

### 编辑体验

- 左侧编辑区按模块分组，支持展开 / 收起
- 顶部工具栏支持示例加载、清空、保存、恢复、JSON 导入导出、PNG 导出、PDF 导出
- 各模块支持独立显隐，关闭后预览区立即同步
- 教育、实习、项目、奖项、证书支持多条目维护
- 各条目支持拖拽排序，也支持上移 / 下移
- 教育、实习、项目、奖项、证书支持按条目隐藏，不删除数据

### 简历内容能力

- 基本信息：姓名、邮箱、电话、个人网站、求职意向
- 教育背景：学校、学历、专业、时间段，支持学校 Logo
- 技术栈：多行输入，支持 `**加粗**` 语法
- 实习经历：公司、岗位、部门、地点、时间、简介、亮点、公司 Logo
- 项目经历：项目名称、角色、周期、标签、简介
- 荣誉奖项：名称、等级、颁发单位、时间、补充描述
- 证书：名称、机构、时间、编号、补充描述
- 自我评价：支持单独隐藏

### 视觉与排版

- 右侧实时生成 A4 风格预览页
- 自动根据主题色生成更深的标题色与姓名 / 学校色
- 支持姓名字体、学校字体、姓名字号、学校字号配置
- 支持证件照尺寸、比例、锁定比例配置
- 支持专业字段加粗显示
- 实习经历支持自定义条目背景色与 Logo 大小
- 预览区检测内容高度，超出一页 A4 时给出提醒

### 本地数据与导出

- 自动保存草稿，默认写入 IndexedDB
- IndexedDB 不可用时回退到 localStorage
- 自动迁移旧版本 localStorage 草稿到 IndexedDB
- 支持导出 JSON 作为可迁移数据文件
- 支持导入 JSON 替换当前简历
- 支持浏览器打印导出 PDF
- 支持通过 SVG + Canvas 导出 PNG

### 图片处理

- 证件照支持 `JPG / PNG / WebP`
- 小于等于 `5MB` 的证件照保留原图
- 超过 `5MB` 的证件照会自动压缩并转换为 JPG
- 学校 Logo 支持 `JPG / PNG / WebP`，限制 `2MB`
- 实习 Logo 支持浏览器可识别的常见图片格式

## 使用场景

- 想在浏览器里快速生成中文技术简历的人
- 希望内容与排版同步编辑，不想来回切文档的人
- 需要本地保存、离线修改、导出投递版 PDF / PNG 的人

## 技术栈

- `Vue 3.5`
- `Vite 8`
- `TDesign Vue Next`
- `Tailwind CSS 4`
- `vuedraggable`
- `JDK 21`
- `Spring Boot 3.5.16`
- `Spring AI Alibaba 1.1.2.2`（已固定 BOM，真实模型 adapter 在后续纵切接入）
- `Maven`

## AI 全栈改造：共享契约基础

前端与 Java 后端共享根目录 `contracts/` 中的 Resume AST 和 ResumePatch JSON Schema。
任何协议修改必须同时通过 Vue/Vitest 与 Java/JUnit 的相同 fixture，避免两端数据模型漂移。

运行环境要求：Node.js `>=22.12`、JDK `21`、Maven `3.9+`。

```bash
./scripts/verify-contracts.sh
./scripts/verify-java.sh
```

ResumePatch 当前只允许有完整证据引用的改写、重组、压缩、删除和已支持关键词抽取。
缺少证据引用会在 Schema 层拒绝；证据覆盖不完整、新增事实和占位指标会被 Policy Guard 拒绝，且不会进入 PatchStore。

## 快速开始

### 安装依赖

```bash
cd apps/web
npm install
```

### 启动开发环境

```bash
cd apps/web
npm run dev
```

### 生产构建

```bash
cd apps/web
npm run build
```

### 预览构建结果

```bash
cd apps/web
npm run preview
```

### 启动 Java Orchestrator

```bash
mvn -pl apps/server -am package
java -jar apps/server/target/server-0.1.0-SNAPSHOT.jar
```

本地默认使用 `LocalDeterministicWorkflow`，不会调用外部模型，也不会生成或编造简历事实。

```bash
curl http://localhost:8080/actuator/health

curl -X POST http://localhost:8080/api/v1/resumes \
  -H 'Content-Type: application/json' \
  --data-binary @contracts/fixtures/resume/valid-minimal-v13.json

curl -X POST http://localhost:8080/api/v1/tasks \
  -H 'Content-Type: application/json' \
  -d '{"resumeId":"res_fixture","baseVersion":1,"jobDescription":"Java Agent Engineer"}'
```

任务只能引用已经导入的不可变 Resume 版本。完整的 Proposal、人工审核、合并和错误协议示例见 [`docs/api/local-review-workflow.md`](docs/api/local-review-workflow.md)。

## 使用说明

1. 打开页面后，先点击“示例”快速了解版式。
2. 在左侧逐项填写或替换内容，右侧会实时更新。
3. 技术栈、实习亮点、项目描述等文本里可使用 `**关键词**` 来强调重点。
4. 如需长期保存，点击“保存”；页面内容修改后也会自动保存。
5. “导出 JSON / 导入 JSON” 用于备份与跨设备迁移。
6. 页面会显示一页高度提醒，可配合 PDF 或 PNG 导出使用。

## 数据模型概览

数据结构由 `normalizeResumeData()` 统一规范化，核心字段包括：

```json
{
  "meta": { "schemaVersion": 8 },
  "profile": {
    "name": "",
    "title": "",
    "phone": "",
    "email": "",
    "website": "",
    "photo": "",
    "photoMeta": null
  },
  "educations": [],
  "skills": "",
  "internships": [],
  "projects": [],
  "awards": [],
  "certificates": [],
  "selfSummary": {
    "content": "",
    "hidden": false
  },
  "sectionVisibility": {},
  "layout": {
    "order": []
  },
  "theme": {
    "primaryColor": "",
    "nameColor": "",
    "schoolColor": "",
    "nameFont": "",
    "nameFontSize": 18,
    "schoolFont": "",
    "schoolFontSize": 13,
    "boldMajor": false,
    "educationFirst": true,
    "photoConfig": {}
  }
}
```

JSON 导入导出是应用层的数据交换入口。

## 项目结构

```text
apps/
  web/                  # Vue 编辑、预览与导出
  server/               # 唯一 Spring Boot 可执行 API
modules/
  resume-domain/        # Policy Guard、ResumePatch、Patch Engine
  task-runtime/         # 任务聚合、状态机、仓储端口
  agent-workflow/       # Agent 端口、Review/Merge 与版本用例
  infrastructure/       # Schema、内存仓储、本地 fake adapter
contracts/              # Vue 与 Java 共用 JSON Schema/fixture
scripts/                # 合同和 Java reactor 验证入口
```

## 实现细节说明

### 1. 持久化策略

- 草稿默认保存到 IndexedDB
- 如果浏览器环境不支持或事务失败，则自动回退到 localStorage
- 老版本 localStorage 草稿会在启动时迁移

### 2. 富文本策略

文本渲染采用轻量规则：

- `**文本**` 会在预览中转换为粗体
- 换行会保留为多行展示
- 标签字段通过逗号分隔

输入方式更接近受限 Markdown，而不是所见即所得编辑器。

### 3. 导出策略

- PDF 导出依赖浏览器打印能力，本质是 `window.print()`
- PNG 导出通过克隆预览 DOM、序列化为 SVG、再绘制到 Canvas

最终导出效果与浏览器实现存在耦合。

## 开发说明

本地模式没有 API Key、数据库、RocketMQ 或 Docker 依赖。真实模型、持久化、消息队列与沙箱都必须通过 infrastructure adapter 接入，不能进入领域模块。

Java 模块说明见 [`docs/architecture/java-modules.md`](docs/architecture/java-modules.md)。

## License

仓库未声明许可证。
