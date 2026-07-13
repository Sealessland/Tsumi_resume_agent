## 1. 数据基础（typography 与 schema 版本）

- [x] 1.1 `src/modules/resume/typography.js`：新增导出 `DEFAULT_LINE_HEIGHT = 1.6`、`LINE_HEIGHT_MIN = 1.2`、`LINE_HEIGHT_MAX = 2.2`；新增 `clampLineHeight(value, fallback = DEFAULT_LINE_HEIGHT)`——外层显式守护 `null`/`undefined`/`''` 走 fallback（`Number(null)===0`、`Number('')===0` 会被 `clampSize` 错误夹到下限），其余复用 `clampSize`。设计见 design §Decisions 2。
- [x] 1.2 `src/modules/resume/constants.js`：将 line 7 的 `export const SCHEMA_VERSION = 11` 改为 `12`。使用点 `templates.js`(40,100)、`normalize.js`(153)、`useResumeBuilder.js`(388) 自动跟随常量，无需额外改动。

## 2. 数据模型与归一化

- [x] 2.1 `src/modules/resume/templates.js`：import 块追加 `DEFAULT_LINE_HEIGHT`；`createEmptyResume().theme` 与 `createDemoResume()` 的 theme 块各新增 8 个 `*LineHeight: DEFAULT_LINE_HEIGHT` 字段（`skillsLineHeight`/`internshipSummaryLineHeight`/`internshipHighlightsLineHeight`/`projectSummaryLineHeight`/`projectHighlightsLineHeight`/`awardDescriptionLineHeight`/`certificateDescriptionLineHeight`/`selfSummaryLineHeight`）。
- [x] 2.2 `src/modules/resume/normalize.js`：import 块追加 `clampLineHeight`；归一化段在 `selfSummaryFontSize` 之后、`boldMajor` 之前新增 8 行 `normalized.theme.{field} = clampLineHeight(normalized.theme.{field})`。`clampLineHeight(undefined)` 经 `clampSize` 走 fallback 返回默认，旧草稿缺失字段自动补 `1.6`，无需显式 `if`。

## 3. CSS 变量下发

- [x] 3.1 `src/composables/useResumeBuilder.js`：import 块追加 `clampLineHeight`；`brandStyle` computed 末尾追加 8 项 `'--{kebab-field}-line-height': String(clampLineHeight(resume.theme.{field})),`（`--skills-line-height`/`--internship-summary-line-height`/`--internship-highlights-line-height`/`--project-summary-line-height`/`--project-highlights-line-height`/`--award-description-line-height`/`--certificate-description-line-height`/`--self-summary-line-height`）。变量经 `App.vue` 根 `:style="brandStyle"` 下发，`ResumePreview` 继承。

## 4. 正文样式切换

- [x] 4.1 `src/style.css`：将 6 处正文 `line-height: 1.6;` 改为 `line-height: var(--{对应}-line-height, 1.6);`——`.skill-line`/`.entry-summary`/`.entry-bullet`/`.project-summary`/`.project-highlight-item`/`.summary-text`。不动 `.resume-section-title`(1.2) 与 Tailwind `leading-*`。
- [x] 4.2 `src/style.css`：删除 `.plain-meta-desc` base class 内的 `line-height: 1.6;`，保留 `@apply text-slate-800`；在 `.plain-meta-desc--award` 新增 `line-height: var(--award-description-line-height, 1.6);`；在 `.plain-meta-desc--certificate` 新增 `line-height: var(--certificate-description-line-height, 1.6);`。

## 5. 编辑器 UI

- [x] 5.1 `src/components/resume/ResumeEditor.vue`：import 块追加 `LINE_HEIGHT_MIN`、`LINE_HEIGHT_MAX`、`clampLineHeight`（`DEFAULT_LINE_HEIGHT` 未在组件内使用，不导入；`clampLineHeight` 用于滑块回调越界兜底，落实 design §Decisions 6 contingency）。
- [x] 5.2 `src/components/resume/ResumeEditor.vue`：局部常量区域新增 `const lineHeightMin = LINE_HEIGHT_MIN`、`const lineHeightMax = LINE_HEIGHT_MAX`、`const lineHeightStep = 0.1`。
- [x] 5.3 `src/components/resume/ResumeEditor.vue`：8 个字号滑块分散在各分区面板（非集中主题面板），每个字号滑块卡片之后插入对应行距滑块卡片。8 个滑块均 `:min="lineHeightMin"` `:max="lineHeightMax"` `:step="lineHeightStep"` `:input-number-props="sliderInputProps"` `@update:model-value="resume.theme.{field} = clampLineHeight($event)"`。标签：「正文行距」(skills)、「简介行距」(internshipSummary/projectSummary)、「亮点行距」(internshipHighlights/projectHighlights)、「描述行距」(awardDescription/certificateDescription)、「正文行距」(selfSummary)。

## 6. 验证

- [x] 6.2 `npm run build` 通过（vite v8.0.0，3822 模块转换，无报错；chunk 大小警告与行距功能无关）。归一化路径分析确认旧草稿兼容：`normalizeResumeData` 的 `clone(createEmptyResume())` 已含 8 个默认字段，`Object.assign` 只复制源 theme 中存在的键不会删除默认键，随后 8 行 `clampLineHeight` 兜底非法值；`bootstrapDraft`/`handleJsonImport`/`createResumeSnapshot`/`restoreFromStorage` 均经 `normalizeResumeData` 处理。
- [x] 6.1 `npm run dev` 启动，浏览器加载示例简历，确认 8 个 `theme.*LineHeight` 默认 `1.6`；分别变更 8 个行距字段，预览区对应模块行高实时变化，其余模块与标题/元信息行距不变。浏览器实测：skills=2.0→25px、internshipSummary=1.3→16.25px、internshipHighlights=1.8→22.5px、projectSummary=2.2→27.5px、projectHighlights=1.7→21.25px（补充项目亮点 DOM 后验证）、awardDescription=1.4→17.5px、certificateDescription=1.9→23.75px、selfSummary=1.2→15px；sectionTitle 保持 19.2px。