# Tsumi Resume Studio

一个基于 `Vue 3 + Vite` 的本地简历构建器，采用「左侧编辑 + 右侧实时预览」的双栏工作流，面向中文技术简历场景。

项目围绕“内容编辑”和“成品导出”两个核心环节组织界面。当前版本已经实现本地草稿保存、实时 A4 预览、JSON 导入导出、PNG / PDF 导出、头像与 Logo 上传、主题配色与字体调节等完整链路。

## 项目定位

- 纯前端本地应用，无后端服务依赖
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

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发环境

```bash
npm run dev
```

### 生产构建

```bash
npm run build
```

### 预览构建结果

```bash
npm run preview
```

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
src/
  components/resume/
    ResumeToolbar.vue   # 顶部工具栏
    ResumeEditor.vue    # 左侧编辑器
    ResumePreview.vue   # 右侧预览页
  composables/
    useResumeBuilder.js # 核心状态与交互逻辑
  modules/resume/
    templates.js        # 示例数据与空白模板
    normalize.js        # 数据兼容与结构规范化
    storage.js          # IndexedDB / localStorage 持久化
    photo.js            # 证件照处理与压缩
    photoConfig.js      # 证件照尺寸与比例
    sections.js         # 模块顺序定义
    color.js            # 主题色派生
    nameFont.js         # 姓名 / 学校字体配置
    typography.js       # 字号约束
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

项目没有额外的环境变量要求，也没有后端服务依赖。克隆后安装依赖即可启动。

## License

仓库未声明许可证。
