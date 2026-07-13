<script setup>
defineEmits([
  'load-demo',
  'clear-all',
  'save-draft',
  'restore-draft',
  'export-json',
  'import-json',
  'export-pdf',
  'expand-all-panels',
  'collapse-all-panels',
])
</script>

<template>
  <header class="glass-card no-print mb-5 p-5 lg:p-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <p class="text-xs uppercase tracking-[0.22em] text-sky-700/70">Resume Builder</p>
        <h1 class="title-font mt-1 text-3xl font-semibold text-slate-800 lg:text-4xl">
          Tsumi Resume Studio
        </h1>
      </div>
      <div class="flex flex-wrap gap-2">
        <button type="button" class="toolbar-btn" @click="$emit('load-demo')">示例</button>
        <button type="button" class="toolbar-btn" @click="$emit('clear-all')">清空</button>
        <button type="button" class="toolbar-btn" @click="$emit('save-draft')">保存</button>
        <button type="button" class="toolbar-btn" @click="$emit('restore-draft')">恢复</button>
        <button type="button" class="toolbar-btn" @click="$emit('expand-all-panels')">全部展开</button>
        <button type="button" class="toolbar-btn" @click="$emit('collapse-all-panels')">全部收起</button>
        <button type="button" class="toolbar-btn" @click="$emit('export-json')">导出 JSON</button>
        <button type="button" class="toolbar-btn" @click="$emit('import-json')">导入 JSON</button>
        <button
          type="button"
          class="toolbar-btn toolbar-btn-primary"
          title="导出 PDF 时请选择 A4、边距'无'、关闭页眉页脚，macOS Safari 请取消勾选'自动缩放'"
          @click="$emit('export-pdf')"
        >
          导出 PDF
        </button>
      </div>
    </div>
  </header>
</template>
