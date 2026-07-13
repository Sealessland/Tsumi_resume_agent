## Why

简历正文（技能行、实习简介/亮点、项目简介/亮点、奖项描述、证书描述、自我评价）的 `line-height` 硬编码为 `1.6`，用户无法调节：内容多时溢出一页、内容少时显得稀疏。规划文档 `docs/简历构建器功能规划.md` §5.2「自定义行距」已将其列为体验增强项。不同模块的行距需求不一致（如"项目亮点要紧凑、自我评价要宽松"），单一全局行距无法满足局部精修，需提供按模块独立的行距控制入口，与现有按模块独立的字号定制能力一致。

## What Changes

- 在 `resume.theme` 上新增 8 个独立数值字段（无单位 CSS 倍数）控制各正文模块行距：`skillsLineHeight`、`internshipSummaryLineHeight`、`internshipHighlightsLineHeight`、`projectSummaryLineHeight`、`projectHighlightsLineHeight`、`awardDescriptionLineHeight`、`certificateDescriptionLineHeight`、`selfSummaryLineHeight`，默认均为 `1.6`。
- `src/modules/resume/typography.js` 新增 `DEFAULT_LINE_HEIGHT=1.6`、`LINE_HEIGHT_MIN=1.2`、`LINE_HEIGHT_MAX=2.2`、`clampLineHeight(value, fallback=DEFAULT_LINE_HEIGHT)`（复用现有 `clampSize`，0.1 步进与字号 clampers 对齐）。
- `createEmptyResume` / `createDemoResume` 的 `theme` 各加 8 个 `*LineHeight: DEFAULT_LINE_HEIGHT`。
- `normalizeResumeData` 对 8 个 `theme.*LineHeight` 各执行 `clampLineHeight` 兜底，缺失或非法值回退默认。
- `useResumeBuilder.js` 的 `brandStyle` 输出 8 个 CSS 变量 `--{kebab}-line-height: String(clampLineHeight(resume.theme.{field}))`，经 `App.vue` 根 `:style="brandStyle"` 下发。
- `src/style.css`：
  - 6 处 body `line-height: 1.6` 改为 `line-height: var(--{对应}-line-height, 1.6)`（`.skill-line`/`.entry-summary`/`.entry-bullet`/`.project-summary`/`.project-highlight-item`/`.summary-text`）。
  - `.plain-meta-desc` base 删除 `line-height: 1.6`；在 `.plain-meta-desc--award` / `.plain-meta-desc--certificate` 两个 BEM 修饰符各加 `line-height: var(--{对应}-line-height, 1.6)`。
- `src/components/resume/ResumeEditor.vue` 主题面板新增 8 个「行距」滑块（每个对应一个模块的字号滑块旁），步长 0.1、范围 1.2–2.2，绑定各自 `theme.*LineHeight`，预览实时联动。
- **BREAKING**：`SCHEMA_VERSION` 由 11 升至 12（`src/modules/resume/constants.js`）。旧草稿经 `normalizeResumeData` 自动补 8 个默认值，不丢数据。
- 无新增依赖；行距变化仍由现有 `.resume-page` 固定 A4 容器承接，不引入分页算法改动。

## Capabilities

### New Capabilities

- `resume-line-spacing`: 按模块独立控制简历正文行距倍数。覆盖 8 个模块：技能行、实习简介、实习亮点、项目简介、项目亮点、奖项描述、证书描述、自我评价；每个模块对应一个 `theme.*LineHeight` 字段，经独立 CSS 变量下发，主题配置面板提供对应滑块入口，预览/导出联动。标题行距（`.resume-section-title` 1.2）与元信息 Tailwind `leading-5` 不在此能力范围，保持不变。

### Modified Capabilities

无。`openspec/specs/` 为空，无既有 spec 需 delta。

## Impact

受影响文件：

- `src/modules/resume/typography.js` — 新增 `DEFAULT_LINE_HEIGHT`、`LINE_HEIGHT_MIN/MAX`、`clampLineHeight`。
- `src/modules/resume/constants.js` — `SCHEMA_VERSION` 11→12。
- `src/modules/resume/templates.js` — `createEmptyResume`/`createDemoResume` 的 `theme` 增 8 个 `*LineHeight` 默认值，新增 `DEFAULT_LINE_HEIGHT` 导入。
- `src/modules/resume/normalize.js` — 归一化段新增 8 处 `theme.*LineHeight` 兜底，新增 `clampLineHeight` 导入。
- `src/composables/useResumeBuilder.js` — `brandStyle` 追加 8 个 `--*-line-height` CSS 变量，新增 `clampLineHeight` 导入。
- `src/style.css` — 6 处正文 `line-height:1.6` 改为 `var(...)`；`.plain-meta-desc` base 删 `line-height`，两个 BEM 修饰符各加 `var(...)`。
- `src/components/resume/ResumeEditor.vue` — 主题面板新增 8 个滑块卡片，新增对应常量/导入。

无新增依赖。每模块行距取值区间 `[1.2, 2.2]`、步长 0.1（与字号滑块粒度一致）。打印路径不变：`@media print` 规则与 `.resume-page` 固定 210×297mm 容器保持原状，行距增大导致溢出仍由现有 `pageOverflow` 检测承接，本变更不引入分页算法改动。