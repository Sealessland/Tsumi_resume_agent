## Context

当前简历正文（技能行、实习简介/亮点、项目简介/亮点、奖项描述、证书描述、自我评价）的 `line-height` 在 `src/style.css` 中硬编码为 `1.6`，用户无法调节。内容多时溢出一页、少时稀疏。主题系统已有 `primaryColor`、各 `*FontSize`、`boldMajor` 等 `resume.theme` 字段，每个字号字段经独立 `clamp*FontSize` 归一化、经 `brandStyle` computed 转独立 CSS 变量下发、由 `normalizeResumeData` 做兜底、`SCHEMA_VERSION` 单点常量驱动版本与迁移。本设计在该体系上为行距新增 8 个独立字段，沿用既有按模块独立模式。

已核实现状：

- `src/modules/resume/typography.js`：已有 `DEFAULT_*_FONT_SIZE`/`*_MIN`/`*_MAX` 常量与 `clampSize(value, min, max, fallback)`（line 35-39，舍入到 0.1：`Math.round(parsed*10)/10`）+ `clamp*FontSize` 系列，但无 `lineHeight`/`line-height` 导出。
- `src/modules/resume/templates.js` line 64-93：`createEmptyResume().theme` 已有 `primaryColor`、各字号字段、`boldMajor`、`educationFirst`、`photoConfig`；`createDemoResume()` line 188-217 平行结构。无 `*LineHeight` 字段。
- `src/modules/resume/normalize.js` line 118-151：`source.theme` 经 `Object.assign(normalized.theme, source.theme)` 后逐字段 `clamp*`。无 `*LineHeight` 归一化。
- `src/modules/resume/constants.js` line 7：`SCHEMA_VERSION = 11`。
- `src/composables/useResumeBuilder.js` line 721-748：`brandStyle` 返回 CSS 变量对象，命名风格 `--{kebab-field}` = JS `theme.{camelField}`，绑定到 `App.vue` line 130 `<div class="app-shell" :style="brandStyle">`，`ResumePreview` 在其内部可继承变量。
- `src/style.css` 中 `line-height: 1.6` 出现于：`.skill-line`(369)、`.entry-summary`(442)、`.entry-bullet`(456)、`.project-summary`(497)、`.project-highlight-item`(511)、`.plain-meta-desc`(567)、`.summary-text`(581)。另 `.resume-section-title` 是 `1.2`(350)、`.entry-brand/.entry-role/.entry-time` 等用 `leading-5`(Tailwind)——这些**不在本变更范围**（仅正文行距，本变更也不动标题/元信息）。
- `.plain-meta-desc` 是 base class（line 565-568），被奖项描述与证书描述共用；`.plain-meta-desc--award`(570-572)、`.plain-meta-desc--certificate`(574-576) 是 BEM 修饰符，各自已有独立 `--*-font-size`。分模块行距拆分奖项/证书需在两个修饰符各加 `line-height`，并删除 base 的 `line-height: 1.6`。
- `src/components/resume/ResumeEditor.vue` line 15-46：已从 `typography.js` 导入各 `clamp*` 与 `*_MIN`/`*_MAX`；line 155-164：局部常量 `*FontSizeMin/Max`；line 1579-1682：主题配置面板，含 `setting-slider` + TDesign `Slider`（`sliderInputProps = { theme:'column', size:'small' }`，line 168-171）。

## Goals / Non-Goals

**Goals:**

- 在 `resume.theme` 上为 8 个正文模块各引入独立 `*LineHeight` 数值字段，分别驱动该模块行距。
- 与现有按模块独立的字号/主题色定制能力一致：常量化限值 + `clamp*` 归一化 + 独立 CSS 变量下发 + 编辑器滑块。
- 旧草稿（`schemaVersion < 12`）经归一化自动补 8 个默认 `*LineHeight`，零数据丢失。
- 打印路径不动：`.resume-page` 固定 A4、`@media print` 规则不变、溢出仍由现有 `pageOverflow` 承接。

**Non-Goals:**

- 不调整标题行距（`.resume-section-title` 1.2）与元信息行距（Tailwind `leading-5`），避免破坏信息条紧凑布局。
- 不引入自动分页或多页排版算法。
- 不调整字号、主题色等其他已有主题字段。
- 不引入"全局基础行距 + 模块 offset"的二级模型——直接 8 个独立字段，与字号字段结构对称，避免增加认知负担。

## Decisions

### 1. 8 个独立字段，命名与字号字段对称

字段及其对应 CSS class / 变量：

| theme 字段 | CSS class | CSS var | 默认 |
|---|---|---|---|
| `skillsLineHeight` | `.skill-line` | `--skills-line-height` | 1.6 |
| `internshipSummaryLineHeight` | `.entry-summary` | `--internship-summary-line-height` | 1.6 |
| `internshipHighlightsLineHeight` | `.entry-bullet` | `--internship-highlights-line-height` | 1.6 |
| `projectSummaryLineHeight` | `.project-summary` | `--project-summary-line-height` | 1.6 |
| `projectHighlightsLineHeight` | `.project-highlight-item` | `--project-highlights-line-height` | 1.6 |
| `awardDescriptionLineHeight` | `.plain-meta-desc--award` | `--award-description-line-height` | 1.6 |
| `certificateDescriptionLineHeight` | `.plain-meta-desc--certificate` | `--certificate-description-line-height` | 1.6 |
| `selfSummaryLineHeight` | `.summary-text` | `--self-summary-line-height` | 1.6 |

命名与既有 `*FontSize` 字段一一对应（如 `skillsFontSize` → `skillsLineHeight`），降低认知负担、保证可枚举性。

**Why 8 个而非 1 个全局：** 不同模块行距需求不一致（项目亮点紧凑、自我评价宽松等局部精修场景），单一全局字段无法表达。沿用项目"按字段独立"的既有模式（字号即如此），结构对称、易扩展。

**Alternatives:**

- 单一 `lineHeight` 全局字段：简单但不能局部精修，违背用户分模块控制诉求。废弃。
- 基础 `lineHeight` + 每模块 offset：二级模型，认知负担高、归一化与 UI 更复杂。废弃。

### 2. 默认 `1.6`，区间 `[1.2, 2.2]`，步长 0.1，复用 `clampSize`

CSS `line-height` 无单位倍数继承行为最佳。`1.6` 即现有硬编码基线，作为默认保持向后视觉一致。`1.2`–`2.2` 覆盖紧凑↔宽松实用区间；上限取 `2.2` 是因为超过此值易破坏 A4 单页排版。

**步长 0.1（非 0.05）**：与字号滑块粒度一致（字号也是 1px 步进），且 `clampSize` 内部 `Math.round(parsed*10)/10` 已舍入到 0.1，0.1 步进与之完美兼容——滑块产生的值（如 `1.3`/`1.7`）经 `clampSize` 不会被二次舍入。0.05 步进会与 `clampSize` 的 0.1 舍入冲突（`1.85` 被舍入为 `1.9`，跳档）。

`clampLineHeight(value, fallback = DEFAULT_LINE_HEIGHT)`：在 `clampSize(value, LINE_HEIGHT_MIN, LINE_HEIGHT_MAX, fallback)` 外层包一层 nullity 守护，**显式**将 `null` / `undefined` / `''` 归入 fallback 而非走 `clampSize` 的 `Number(value)`——因为 `Number(null)===0`、`Number('')===0` 都是有限数字会被 `clampSize` 夹到下限 `1.2`，违背 spec「值缺失/非数字 → 默认 1.6」语义。`'abc'`、`NaN` 经 `Number→NaN→!isFinite` 已自动走 fallback，无需单独守护。其余逻辑（舍入到 0.1、夹紧区间）复用 `clampSize`，与 `clamp*FontSize` 同构。

**Alternatives:**

- 0.05 步进 + 独立 clamp（不调 `clampSize` 的舍入）：粒度更细但与字号滑块风格不一致，且需新写 clamp 逻辑。废弃。
- 0.05 步进 + 复用 `clampSize`：`1.85` 被舍入为 `1.9`，跳档 bug。废弃。

### 3. 归一化在 `Object.assign` 之后逐字段兜底

`normalize.js` line 118 `Object.assign(normalized.theme, source.theme)` 后逐字段 `clamp*`。在归一化段末尾（紧跟 `selfSummaryFontSize` 处理之后、`boldMajor` 之前）追加 8 行 `normalized.theme.{field} = clampLineHeight(normalized.theme.{field})`。`clampLineHeight(undefined)` 经 `clampSize` 走 fallback 分支返回 `DEFAULT_LINE_HEIGHT`，故旧草稿缺失字段自动补默认，无需显式 `if`。

### 4. 8 个 CSS 变量经 `brandStyle` 下发

`brandStyle`（line 721-748）末尾追加 8 项 `'--{kebab-field}-line-height': String(clampLineHeight(resume.theme.{field})),`。变量经 `App.vue` line 130 根 `:style="brandStyle"` 下发，`ResumePreview` 在其内部继承。各 class 用 `line-height: var(--{对应}-line-height, 1.6)`，保留 `1.6` 兜底以防变量缺失。

**Why 经变量而非内联 style：** 8 个 class 分散在 `ResumePreview` 多处模板，内联需逐元素 `:style` 绑定且无法覆盖 CSS 类内已有 `line-height`；独立 CSS 变量集中维护、兜底语义清晰、与字号变量下发模式对称。

### 5. `.plain-meta-desc` base 删 line-height，两个 BEM 修饰符各加

CSS 现状：`.plain-meta-desc`（line 565-568）含 `line-height: 1.6`，被 `--award`/`--certificate` 两个修饰符继承。分模块拆分需：

- 删除 `.plain-meta-desc` base 的 `line-height: 1.6`（line 567），保留其余（`@apply text-slate-800`）。
- `.plain-meta-desc--award`（line 570-572）加 `line-height: var(--award-description-line-height, 1.6);`。
- `.plain-meta-desc--certificate`（line 574-576）加 `line-height: var(--certificate-description-line-height, 1.6);`。

其余 6 处直接替换 `line-height: 1.6` 为 `var(--{对应}-line-height, 1.6)`。

### 6. 编辑器 8 个滑块不包 `clampLineHeight`，仅 normalize 兜底

TDesign `Slider` 受 `:min`/`:max` 限制，拖动不会产生越界值，故 `@update:model-value="resume.theme.{field} = $event"` 直接赋值。`normalizeResumeData` 在草稿加载/导出时统一兜底。

**contingency**：若实测附属输入框允许键入越界值，则在 `@update:model-value` 包 `resume.theme.{field} = clampLineHeight($event)`。

### 7. `SCHEMA_VERSION` 11→12

`constants.js` 单点常量。使用点已核实：`constants.js`(7)、`templates.js`(40,100)、`normalize.js`(153)、`useResumeBuilder.js`(388)。升至 12 后自动跟随。旧草稿加载时 `normalizeResumeData` 补 8 个 `*LineHeight`，导出 JSON 携带 `schemaVersion: 12` 与新字段。

**Why 升版本：** 项目惯例「新增 theme 字段即升 `SCHEMA_VERSION`」。`normalize` 兜底保证不丢数据，版本号是语义标记便于后续辨识草稿是否含 `*LineHeight`。

## Risks / Trade-offs

- **8 个滑块面板臃肿**：主题面板将多出 8 个滑块卡片。Trade-off：与现有字号滑块对称排列，每个行距滑块紧跟对应字号滑块，用户认知负担与字号配置相当；不调的模块保持默认 `1.6` 即可。
- **行距过大溢出单页**：用户把某模块设 `2.2` 且内容多时可能超 A4 单页。Trade-off：不引入自动分页（项目当前无此能力），由现有 `pageOverflow` 检测提示，用户自行调小行距或精简内容。可接受，因本能力目标正是让用户在「行距↔单页容量」间取舍。
- **手动输入框越界**（contingency）：若 TDesign Slider 附属输入框允许键入越界值，仅靠 Slider `min/max` 关不住。缓解：实测发现则在 `@update:model-value` 包 `clampLineHeight($event)`。
- **变量缺失兜底**：极端情况下 `--*-line-height` 变量未下发时正文回退 `1.6`，即现状硬编码值，视觉无回退差异。Trade-off：`var(..., 1.6)` 双写默认值需与 `DEFAULT_LINE_HEIGHT` 保持同步——已在 spec 与 tasks 中明确兜底值取默认 `1.6`，未来若改默认值需同步 CSS 兜底字面量。
- **区间对部分用户偏窄**：若用户需要更宽（如 `2.5`）或更窄（如 `1.0`），需改 `LINE_HEIGHT_MIN/MAX`。Trade-off：单一常量点，调整一处即可，8 个字段共用区间不影响其余设计。