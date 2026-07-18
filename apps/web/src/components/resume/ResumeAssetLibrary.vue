<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  assets: { type: Array, required: true },
  currentAssetId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['create', 'select', 'duplicate', 'archive', 'restore', 'delete', 'update-metadata', 'close'])
const query = ref('')
const status = ref('all')
const detailId = ref('')
const detail = computed(() => props.assets.find((asset) => asset.id === detailId.value))
const statusLabel = { draft: '草稿', active: '使用中', archived: '已归档' }

const filtered = computed(() => {
  const needle = query.value.trim().toLowerCase()
  return props.assets.filter((asset) => {
    if (status.value !== 'all' && asset.status !== status.value) return false
    if (!needle) return true
    return [asset.title, asset.targetCompany, asset.targetRole, asset.jobDescription, ...(asset.tags || [])]
      .some((value) => String(value || '').toLowerCase().includes(needle))
  })
})

function update(field, event) {
  emit('update-metadata', detail.value.id, { [field]: event.target.value })
}

function updateTags(event) {
  emit('update-metadata', detail.value.id, {
    tags: event.target.value.split(/[,，]/).map((tag) => tag.trim()).filter(Boolean),
  })
}

function summary(value) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  return text ? `${text.slice(0, 90)}${text.length > 90 ? '…' : ''}` : '尚未绑定 JD'
}
</script>

<template>
  <section class="asset-library no-print" aria-label="岗位简历资产库">
    <header class="asset-header">
      <div>
        <p class="asset-eyebrow">RESUME ASSETS · {{ assets.length }} 份档案</p>
        <h2 class="title-font">简历资产库</h2>
        <p>以岗位为单位维护版本、JD 与投递状态。选中后，编辑器与 AI 同步切换。</p>
      </div>
      <button type="button" class="asset-close" aria-label="关闭资产库" @click="emit('close')">×</button>
    </header>

    <div class="asset-primary-actions">
      <div><strong>新建一份岗位版本</strong><span>从空白内容或示例内容开始，避免覆盖现有简历。</span></div>
      <div class="asset-actions">
        <button type="button" class="toolbar-btn" @click="emit('create', 'demo')">使用示例新建</button>
        <button type="button" class="toolbar-btn toolbar-btn-primary" @click="emit('create', 'blank')">+ 新建空白版本</button>
      </div>
    </div>

    <div class="asset-filter">
      <input v-model="query" class="field-input" type="search" placeholder="搜索公司、岗位、JD 或标签" />
      <div class="asset-statuses" aria-label="状态筛选">
        <button v-for="item in [['all', '全部'], ['active', '使用中'], ['draft', '草稿'], ['archived', '归档']]" :key="item[0]" type="button" :class="{ selected: status === item[0] }" @click="status = item[0]">{{ item[1] }}</button>
      </div>
    </div>

    <p v-if="loading" class="asset-empty">正在读取本地岗位档案…</p>
    <div v-else-if="filtered.length" class="asset-list">
      <article v-for="asset in filtered" :key="asset.id" class="asset-card" :class="{ current: asset.id === currentAssetId }">
        <div class="asset-card__head">
          <div>
            <p class="asset-card__type">{{ statusLabel[asset.status] }}</p>
            <h3>{{ asset.title || '未命名岗位简历' }}</h3>
            <p class="asset-card__target">{{ asset.targetCompany || '未填写公司' }} <span>/</span> {{ asset.targetRole || '未填写岗位' }}</p>
          </div>
          <span v-if="asset.id === currentAssetId" class="current-badge">AI 处理目标</span>
        </div>
        <p class="asset-card__jd">{{ summary(asset.jobDescription) }}</p>
        <div class="asset-card__tags"><span v-for="tag in asset.tags" :key="tag">{{ tag }}</span></div>
        <div class="asset-card__actions">
          <button v-if="asset.id !== currentAssetId" type="button" class="toolbar-btn toolbar-btn-primary" @click="emit('select', asset.id)">设为当前并编辑</button>
          <span v-else class="asset-card__current">正在编辑与 AI 优化</span>
          <button type="button" class="small-btn" @click="detailId = asset.id">编辑岗位信息与 JD</button>
          <button type="button" class="small-btn" @click="emit('duplicate', asset.id)">复制为新版本</button>
          <button v-if="asset.status === 'archived'" type="button" class="small-btn" @click="emit('restore', asset.id)">恢复档案</button>
          <button v-else type="button" class="small-btn" @click="emit('archive', asset.id)">归档</button>
          <button type="button" class="small-btn small-btn-danger" @click="emit('delete', asset.id)">删除</button>
        </div>
      </article>
    </div>
    <div v-else class="asset-empty">没有符合条件的岗位档案。新建一份简历，并在详情中粘贴 JD。</div>

    <div v-if="detail" class="detail-mask" @click.self="detailId = ''">
      <aside class="detail-drawer" aria-label="岗位简历详情">
        <header class="asset-header">
          <div><p class="asset-eyebrow">JOB BRIEF</p><h2 class="title-font">岗位档案详情</h2></div>
          <button type="button" class="asset-close" aria-label="关闭详情" @click="detailId = ''">×</button>
        </header>
        <div class="detail-form">
          <p class="detail-hint">这里维护该版本的目标公司、岗位与 JD；AI 会直接使用这份 JD。</p>
          <label class="field-wrap"><span class="field-label">简历名称</span><input :value="detail.title" class="field-input" @input="update('title', $event)" /></label>
          <div class="grid gap-4 sm:grid-cols-2">
            <label class="field-wrap"><span class="field-label">目标公司</span><input :value="detail.targetCompany" class="field-input" @input="update('targetCompany', $event)" /></label>
            <label class="field-wrap"><span class="field-label">目标岗位</span><input :value="detail.targetRole" class="field-input" @input="update('targetRole', $event)" /></label>
          </div>
          <label class="field-wrap"><span class="field-label">标签</span><input :value="(detail.tags || []).join('，')" class="field-input" placeholder="Vue，校招，上海" @input="updateTags" /></label>
          <label class="field-wrap"><span class="field-label">岗位 JD</span><textarea :value="detail.jobDescription" class="field-input detail-jd" maxlength="20000" placeholder="粘贴岗位职责和任职要求" @input="update('jobDescription', $event)"></textarea></label>
          <div class="detail-actions"><button type="button" class="toolbar-btn toolbar-btn-primary" :disabled="detail.id === currentAssetId" @click="emit('select', detail.id); detailId = ''">{{ detail.id === currentAssetId ? '当前 AI 处理目标' : '设为当前 AI 处理目标' }}</button><button type="button" class="toolbar-btn" @click="detailId = ''">完成</button></div>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.asset-library { min-height: 100%; padding: 30px; background: #fbfdff; color: #17243a; }
.asset-header { display: flex; justify-content: space-between; gap: 20px; border-bottom: 1px solid #e4edf5; padding-bottom: 20px; }
.asset-eyebrow { margin: 0 0 6px; color: #5390bf; font-size: 10px; font-weight: 800; letter-spacing: .18em; }
h2 { margin: 0; font-size: 25px; } .asset-header p:not(.asset-eyebrow) { margin: 8px 0 0; color: #64748b; font-size: 13px; line-height: 1.7; }
.asset-close { width: 34px; height: 34px; border-radius: 50%; color: #64748b; font-size: 24px; line-height: 1; } .asset-close:hover { background: #edf6fc; }
.asset-primary-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 15px; margin: 22px 0; border: 1px solid #c6e5f4; border-radius: 14px; background: #f2faff; padding: 14px; } .asset-primary-actions strong { display: block; color: #14567b; font-size: 14px; } .asset-primary-actions span { display: block; margin-top: 3px; color: #698397; font-size: 12px; } .asset-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.asset-filter { display: grid; gap: 12px; } .asset-statuses { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 4px; }
.asset-statuses button { flex: 0 0 auto; border-radius: 999px; padding: 6px 11px; color: #64748b; font-size: 12px; } .asset-statuses button.selected { background: #e0f2fe; color: #0369a1; font-weight: 700; }
.asset-list { display: grid; gap: 14px; margin-top: 20px; } .asset-card { border: 1px solid #e1ebf3; border-left: 4px solid #94a3b8; border-radius: 14px; background: #fff; padding: 17px; } .asset-card.current { border-left-color: #0ea5e9; box-shadow: 0 12px 28px -24px rgba(2,132,199,.8); }
.asset-card__head { display: flex; justify-content: space-between; gap: 14px; } .asset-card__type { margin: 0 0 4px; color: #7691a8; font-size: 11px; font-weight: 700; } h3 { margin: 0; color: #17243a; font-size: 16px; } .asset-card__target { margin: 7px 0 0; color: #64748b; font-size: 13px; } .asset-card__target span { margin: 0 5px; color: #c3d0db; }
.current-badge { align-self: flex-start; border-radius: 999px; background: #e0f8ef; padding: 5px 8px; color: #087f5b; font-size: 11px; font-weight: 700; white-space: nowrap; } .asset-card__jd { margin: 14px 0 0; color: #50677b; font-size: 13px; line-height: 1.65; } .asset-card__tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; } .asset-card__tags span { border-radius: 6px; background: #f1f6fa; padding: 3px 7px; color: #5b7287; font-size: 11px; } .asset-card__actions { display: flex; flex-wrap: wrap; align-items: center; gap: 7px; margin-top: 16px; } .asset-card__current { border-radius: 7px; background: #e6f8ef; padding: 7px 9px; color: #087f5b; font-size: 12px; font-weight: 800; }
.asset-empty { margin: 30px 0; border: 1px dashed #cbd9e5; border-radius: 14px; padding: 28px; color: #64748b; text-align: center; font-size: 13px; }
.detail-mask { position: fixed; z-index: 80; inset: 0; background: rgba(15,23,42,.2); backdrop-filter: blur(2px); } .detail-drawer { position: absolute; top: 0; right: 0; width: min(520px, 94vw); height: 100%; overflow-y: auto; background: #fff; box-shadow: -24px 0 70px -35px rgba(15,47,77,.5); padding: 30px; } .detail-form { display: grid; gap: 18px; padding-top: 24px; } .detail-hint { margin: 0; border-radius: 12px; background: #f0f8ff; padding: 12px; color: #46647e; font-size: 13px; line-height: 1.65; } .detail-jd { min-height: 310px; resize: vertical; line-height: 1.7; } .detail-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; border-top: 1px solid #e4edf5; padding-top: 18px; }
</style>
