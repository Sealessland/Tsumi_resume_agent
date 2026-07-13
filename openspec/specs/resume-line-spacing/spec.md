# Resume Line Spacing Specification

## Purpose

Define per-module line-height customization for resume body content, including persistence, import/export compatibility, preview styling, editor controls, and print-container behavior.

## Requirements

### Requirement: 行距字段默认值

`resume.theme` SHALL 包含 8 个独立行距数值字段（无单位 CSS 倍数），默认值 SHALL 均为 `1.6`，与项目既有正文行距基线一致：`skillsLineHeight`（技能行）、`internshipSummaryLineHeight`（实习简介）、`internshipHighlightsLineHeight`（实习亮点）、`projectSummaryLineHeight`（项目简介）、`projectHighlightsLineHeight`（项目亮点）、`awardDescriptionLineHeight`（奖项描述）、`certificateDescriptionLineHeight`（证书描述）、`selfSummaryLineHeight`（自我评价）。

#### Scenario: 空简历携带 8 个默认行距

- **WHEN** 调用 `createEmptyResume()` 创建新简历
- **THEN** 返回的 `resume.theme` 中上述 8 个 `*LineHeight` 字段 SHALL 全部等于 `1.6`

#### Scenario: 示例简历携带 8 个默认行距

- **WHEN** 调用 `createDemoResume()` 创建示例简历
- **THEN** 返回的 `resume.theme` 中上述 8 个 `*LineHeight` 字段 SHALL 全部等于 `1.6`

### Requirement: 行距取值区间

每个 `theme.*LineHeight` MUST 限制在闭区间 `[1.2, 2.2]`。任何越界、非数字或缺失值 MUST 回退到默认值 `1.6`（越界则夹紧到对应边界，非数字/缺失则取默认）。归一化后值舍入到 0.1 精度（与现有字号归一化一致）。

#### Scenario: 值缺失回退默认

- **WHEN** 输入草稿的某 `theme.*LineHeight` 字段缺失（如 `undefined`）
- **THEN** 归一化后该字段 SHALL 等于 `1.6`

#### Scenario: 非数字值回退默认

- **WHEN** 输入草稿的某 `theme.*LineHeight` 为非数字（如字符串、`NaN`、`null`）
- **THEN** 归一化后该字段 SHALL 等于 `1.6`

#### Scenario: 值低于下限夹紧

- **WHEN** 输入某 `theme.*LineHeight` 为 `1.0`（小于下限 `1.2`）
- **THEN** 归一化后该字段 SHALL 等于 `1.2`

#### Scenario: 值高于上限夹紧

- **WHEN** 输入某 `theme.*LineHeight` 为 `2.5`（大于上限 `2.2`）
- **THEN** 归一化后该字段 SHALL 等于 `2.2`

#### Scenario: 合法值原样保留

- **WHEN** 输入某 `theme.*LineHeight` 为 `1.8`（位于 `[1.2, 2.2]` 内且为 0.1 整数倍）
- **THEN** 归一化后该字段 SHALL 等于 `1.8`

### Requirement: 旧草稿归一化兼容

`schemaVersion` 小于 12 的旧草稿（不含任何 `*LineHeight` 字段）经 `normalizeResumeData` 处理后，8 个 `theme.*LineHeight` MUST 均为合法值（缺失则默认 `1.6`），不丢失草稿其余数据。

#### Scenario: schemaVersion 11 草稿加载

- **WHEN** 加载一份 `schemaVersion: 11` 且 `theme` 无任何 `*LineHeight` 字段的旧草稿
- **THEN** `normalizeResumeData` 输出的 8 个 `theme.*LineHeight` SHALL 全部等于 `1.6`
- **AND** 草稿其余字段（姓名、经历、主题色、字号等）SHALL 保持原值

### Requirement: CSS 变量独立下发

`useResumeBuilder` 的 `brandStyle` computed SHALL 输出 8 个独立 CSS 变量，每变量值等于对对应 `theme.*LineHeight` 做合法区间夹紧后得到的数值：`--skills-line-height`、`--internship-summary-line-height`、`--internship-highlights-line-height`、`--project-summary-line-height`、`--project-highlights-line-height`、`--award-description-line-height`、`--certificate-description-line-height`、`--self-summary-line-height`。变量经应用根节点 `:style="brandStyle"` 下发，可被预览区样式继承。

#### Scenario: 变量值随主题行距变化

- **WHEN** `resume.theme.skillsLineHeight` 设为 `1.8`、`resume.theme.selfSummaryLineHeight` 设为 `2.0`
- **THEN** `brandStyle` 中 `--skills-line-height` SHALL 等于 `"1.8"`
- **AND** `--self-summary-line-height` SHALL 等于 `"2.0"`

#### Scenario: 变量兜底未受影响

- **WHEN** 预览区某元素直接使用 `var(--skills-line-height, 1.6)` 且变量因故未下发
- **THEN** 该元素行距 SHALL 回退到 `1.6`

### Requirement: 正文元素行距使用各自主题变量

以下 8 处正文元素的 CSS `line-height` MUST 取自对应的 `var(--{对应}-line-height, 1.6)`，跟随各自 `theme.*LineHeight` 实时变化：

- `.skill-line` → `--skills-line-height`
- `.entry-summary` → `--internship-summary-line-height`
- `.entry-bullet` → `--internship-highlights-line-height`
- `.project-summary` → `--project-summary-line-height`
- `.project-highlight-item` → `--project-highlights-line-height`
- `.plain-meta-desc--award` → `--award-description-line-height`
- `.plain-meta-desc--certificate` → `--certificate-description-line-height`
- `.summary-text` → `--self-summary-line-height`

`.plain-meta-desc` base class MUST NOT 保留 `line-height` 声明（由两个 BEM 修饰符各自接管）。标题（`.resume-section-title`，`1.2`）与元信息（`.entry-brand`/`.entry-role`/`.entry-time` 等 Tailwind `leading-5`）不在本要求范围，保持原值不变。

#### Scenario: 调节某模块行距仅影响该模块

- **WHEN** 用户把 `theme.skillsLineHeight` 从 `1.6` 调到 `2.0`，其余 `*LineHeight` 保持 `1.6`
- **THEN** `.skill-line` 的计算 `line-height` SHALL 等于 `2.0`
- **AND** 其余 7 处正文元素的计算 `line-height` SHALL 保持 `1.6`
- **AND** `.resume-section-title` 的 `line-height` SHALL 保持 `1.2`

#### Scenario: 奖项与证书行距独立

- **WHEN** 用户把 `theme.awardDescriptionLineHeight` 设为 `1.4`、`theme.certificateDescriptionLineHeight` 设为 `1.8`
- **THEN** `.plain-meta-desc--award` 的 `line-height` SHALL 等于 `1.4`
- **AND** `.plain-meta-desc--certificate` 的 `line-height` SHALL 等于 `1.8`

#### Scenario: 变量缺失时正文回退

- **WHEN** `--skills-line-height` 变量未下发（如品牌样式未应用）
- **THEN** `.skill-line` 的 `line-height` SHALL 回退到 `1.6`

### Requirement: 主题面板提供 8 个行距滑块

简历编辑器主题配置面板 SHALL 为 8 个模块各提供一个行距滑块控件：每个滑块范围 `1.2`–`2.2`、步长 `0.1`，双向绑定对应 `resume.theme.*LineHeight`。调节任一滑块时，预览区对应模块的行距 MUST 实时变化，无需保存或刷新。8 个滑块各自独立，调节一个不影响其余。

#### Scenario: 拖动技能行距滑块仅更新技能行

- **WHEN** 用户在主题面板把「技能行距」滑块从 `1.6` 拖到 `1.8`
- **THEN** `resume.theme.skillsLineHeight` SHALL 变为 `1.8`
- **AND** 预览区 `.skill-line` 行距 SHALL 立即变为 `1.8`
- **AND** 其余 7 个 `*LineHeight` 字段 SHALL 保持不变

#### Scenario: 滑块边界约束

- **WHEN** 用户尝试将任一滑块拖至低于 `1.2` 或高于 `2.2`
- **THEN** 该滑块 `model-value` SHALL 被限制在 `[1.2, 2.2]` 范围内

### Requirement: 行距不破坏打印分页

任一模块的行距变化 MUST NOT 改变打印 A4 分页容器行为：`.resume-page` 固定 210×297mm，`@media print` 规则保持原状。行距增大导致内容超出单页时，由现有溢出检测承接，本能力 MUST NOT 引入新的分页算法或修改既有分页规则。

#### Scenario: 打印容器尺寸不变

- **WHEN** 任一 `theme.*LineHeight` 在 `[1.2, 2.2]` 范围内任意取值
- **THEN** `.resume-page` 容器尺寸 SHALL 保持 210×297mm
- **AND** `@media print` 既有规则 SHALL 不变

#### Scenario: 行距增大的溢出承接

- **WHEN** 某模块 `*LineHeight` 设为 `2.2` 导致内容超出单页
- **THEN** 系统 SHALL 通过现有溢出检测机制提示超出
- **AND** MUST NOT 引入新的自动分页行为
