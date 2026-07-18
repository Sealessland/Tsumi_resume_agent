<script setup>
defineProps({})

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
  <header class="workspace-header no-print mb-5">
    <div class="workspace-header__identity">
      <p class="workspace-header__eyebrow">Tsumi Resume Studio</p>
      <h1 class="title-font">岗位简历工作台</h1>
    </div>
    <div class="workspace-header__actions">
      <details class="toolbar-menu">
        <summary class="toolbar-btn list-none">工作台操作</summary>
        <div class="toolbar-menu__items">
          <button type="button" @click="$emit('save-draft')">立即保存</button>
          <button type="button" @click="$emit('restore-draft')">恢复草稿</button>
          <button type="button" @click="$emit('load-demo')">载入示例</button>
          <button type="button" @click="$emit('expand-all-panels')">展开全部</button>
          <button type="button" @click="$emit('collapse-all-panels')">收起全部</button>
          <button type="button" @click="$emit('import-json')">导入 JSON</button>
          <button type="button" @click="$emit('export-json')">导出 JSON</button>
          <button type="button" class="text-rose-600" @click="$emit('clear-all')">清空当前内容</button>
        </div>
      </details>
      <button type="button" class="toolbar-btn toolbar-btn-primary" @click="$emit('export-pdf')">导出 PDF</button>
    </div>
  </header>
</template>

<style scoped>
.workspace-header { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 20px; border: 1px solid #dce9f3; border-radius: 20px; background: linear-gradient(115deg, #f8fcff 0%, #fff 54%, #f0f9ff 100%); padding: 18px 20px; box-shadow: 0 16px 38px -32px rgba(15, 78, 128, .52); }
.workspace-header__identity { min-width: 190px; } .workspace-header__eyebrow { margin: 0 0 6px; color: #3180b5; font-size: 10px; font-weight: 800; letter-spacing: .18em; text-transform: uppercase; } h1 { margin: 0; color: #17243a; font-size: clamp(24px, 3vw, 32px); line-height: 1.1; }
.workspace-header__actions { display: flex; flex: 1; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 9px; }
.toolbar-menu { position: relative; } .toolbar-menu__items { position: absolute; z-index: 30; top: calc(100% + 7px); right: 0; display: grid; width: 170px; border: 1px solid #dbe8f3; border-radius: 10px; background: white; padding: 5px; box-shadow: 0 16px 35px -22px rgba(15,47,77,.45); } .toolbar-menu__items button { border-radius: 7px; padding: 8px; text-align: left; font-size: 12px; } .toolbar-menu__items button:hover { background: #f1f7fb; }
@media (max-width: 720px) { .workspace-header { padding: 16px; } .workspace-header__actions { justify-content: stretch; } .toolbar-menu, .toolbar-btn-primary { flex: 1 1 auto; text-align: center; } }
</style>
