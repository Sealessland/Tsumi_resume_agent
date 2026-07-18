<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { createAgentApi } from '../../modules/agent/api'
import { createResumeEnvelope, validateResume } from '../../modules/resume/contracts'

const props = defineProps({
  resume: {
    type: Object,
    required: true,
  },
  jobDescription: {
    type: String,
    default: '',
  },
  assetLabel: {
    type: String,
    default: '当前岗位简历',
  },
})

const emit = defineEmits(['apply-merged-resume', 'close'])
const api = createAgentApi()
const backendStatus = ref('checking')
const useTemporaryJobDescription = ref(false)
const localJobDescription = ref('')
const task = ref(null)
const surface = ref(null)
const busy = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const editDrafts = reactive({})
let pollTimer = null

const activeStatuses = new Set(['CREATED', 'ANALYZING', 'PROPOSING', 'VERIFYING'])
const statusLabels = {
  CREATED: '任务已创建',
  ANALYZING: '正在分析职位要求',
  PROPOSING: '正在生成优化建议',
  VERIFYING: '正在核验证据',
  REVIEW_READY: '等待人工审核',
  APPROVED: '已批准，等待合并',
  COMPLETED: '已合并完成',
  FAILED: '执行失败',
  CANCELLED: '已取消',
}

const taskStatusLabel = computed(() => {
  if (!task.value) return '尚未创建任务'
  return statusLabels[task.value.status] || task.value.status
})
const patches = computed(() => surface.value?.patches || [])
const gaps = computed(() => surface.value?.gaps || [])
const timeline = computed(() => (surface.value?.timeline || []).slice(-8).reverse())
const canMerge = computed(() => surface.value?.actions?.includes('MERGE'))
const effectiveJobDescription = computed(() => useTemporaryJobDescription.value
  ? localJobDescription.value.trim()
  : props.jobDescription.trim())


const jobDescriptionLength = computed(() => effectiveJobDescription.value.length)

const jobDescriptionHint = computed(() => {
  if (!jobDescriptionLength.value) return '粘贴完整 JD，AI 才能定位职位要求。'
  if (jobDescriptionLength.value < 80) return 'JD 信息较少，建议补充职责、要求和技能关键词。'
  return `已准备 ${jobDescriptionLength.value} 字 JD，将据此生成可审核建议。`
})

const intentLabels = {
  PARAPHRASE: '措辞优化',
  RESTRUCTURE: '结构重组',
  COMPRESS: '压缩表达',
  DELETE: '删除冗余',
  EXTRACT_SUPPORTED_KEYWORD: '提取匹配关键词',
}

function confidenceLabel(value) {
  const confidence = Number(value)
  if (!Number.isFinite(confidence)) return '置信度未知'
  return `置信度 ${Math.round(confidence * 100)}%`
}
function makeResumeId() {
  const random = globalThis.crypto?.randomUUID?.().replaceAll('-', '')
    || `${Date.now()}${Math.random().toString(36).slice(2)}`
  return `res_web_${random}`
}

function clearFeedback() {
  errorMessage.value = ''
  statusMessage.value = ''
}

function presentError(error) {
  console.error('Agent workspace request failed:', error)
  errorMessage.value = error?.code
    ? `${error.message}（${error.code}）`
    : (error?.message || 'Agent 请求失败，请稍后重试。')
}

function formatOccurredAt(value) {
  const numeric = Number(value)
  const timestamp = Number.isFinite(numeric) && numeric < 1_000_000_000_000
    ? numeric * 1000
    : value
  const date = new Date(timestamp)
  return Number.isNaN(date.getTime()) ? '时间未知' : date.toLocaleTimeString()
}

async function checkBackend() {
  backendStatus.value = 'checking'
  try {
    const health = await api.health()
    backendStatus.value = health?.status === 'UP' ? 'online' : 'offline'
  } catch (error) {
    backendStatus.value = 'offline'
  }
}

function stopPolling() {
  if (pollTimer) window.clearTimeout(pollTimer)
  pollTimer = null
}

function schedulePoll() {
  stopPolling()
  pollTimer = window.setTimeout(refreshTask, 900)
}

async function loadReviewSurface() {
  if (!task.value?.taskId) return
  surface.value = await api.getReviewSurface(task.value.taskId)
  surface.value.patches.forEach((patch) => {
    if (!(patch.patchId in editDrafts)) editDrafts[patch.patchId] = patch.after
  })
  if (!surface.value.patches.length && task.value.status === 'REVIEW_READY') {
    statusMessage.value = task.value.workflowSummary === 'LOCAL_FAKE_READY_FOR_REVIEW'
      ? '后端当前运行在 local 确定性占位模式，不会生成优化建议；请使用 ai profile 启动真实模型工作流。'
      : '任务已完成，但 Evidence Guard 没有放行任何修改；请查看覆盖缺口和时间线。'
  }
}

async function refreshTask() {
  if (!task.value?.taskId) return
  try {
    task.value = await api.getTask(task.value.taskId)
    if (activeStatuses.has(task.value.status)) {
      schedulePoll()
      return
    }
    stopPolling()
    await loadReviewSurface()
  } catch (error) {
    stopPolling()
    presentError(error)
  }
}

async function startTask() {
  clearFeedback()
  if (!effectiveJobDescription.value) {
    errorMessage.value = useTemporaryJobDescription.value
      ? '请粘贴临时 JD，或切回岗位档案中的 JD。'
      : '当前岗位档案未填写 JD；请填写后再分析，或使用临时 JD。'
    return
  }

  busy.value = true
  surface.value = null
  task.value = null
  Object.keys(editDrafts).forEach((key) => delete editDrafts[key])
  try {
    await checkBackend()
    if (backendStatus.value !== 'online') {
      throw new Error('Agent 后端未启动，请先启动 8080 服务。')
    }
    const resumeId = makeResumeId()
    const envelope = createResumeEnvelope(props.resume, { resumeId, version: 1 })
    const validation = validateResume(envelope)
    if (!validation.valid) {
      throw new Error(`当前简历不符合后端合同：${validation.errors[0]?.message || '校验失败'}`)
    }
    await api.importResume(envelope)
    task.value = await api.createTask({
      resumeId,
      baseVersion: 1,
      jobDescription: effectiveJobDescription.value,
    })
    statusMessage.value = `「${props.assetLabel}」已安全导入，Agent 任务开始执行。`
    schedulePoll()
  } catch (error) {
    presentError(error)
  } finally {
    busy.value = false
  }
}

async function decidePatch(patch, decision) {
  clearFeedback()
  busy.value = true
  try {
    await api.decidePatch(task.value.taskId, patch.patchId, patch.expectedBaseVersion, decision)
    await refreshTask()
    statusMessage.value = decision === 'ACCEPTED' ? '已接受该建议。' : '已拒绝该建议。'
  } catch (error) {
    presentError(error)
  } finally {
    busy.value = false
  }
}

async function editPatch(patch) {
  clearFeedback()
  busy.value = true
  try {
    await api.editPatch(
      task.value.taskId,
      patch.patchId,
      patch.expectedBaseVersion,
      editDrafts[patch.patchId],
    )
    await refreshTask()
    statusMessage.value = '人工编辑已重新通过 Evidence Guard，请审核新版本。'
  } catch (error) {
    presentError(error)
  } finally {
    busy.value = false
  }
}

async function mergeAccepted() {
  clearFeedback()
  busy.value = true
  try {
    const merged = await api.mergeTask(task.value.taskId, surface.value.baseVersion)
    emit('apply-merged-resume', merged)
    task.value = await api.getTask(task.value.taskId)
    await loadReviewSurface()
    statusMessage.value = `已生成后端简历 v${merged.version}，并同步回当前编辑器。`
  } catch (error) {
    presentError(error)
  } finally {
    busy.value = false
  }
}

async function cancelTask() {
  busy.value = true
  try {
    task.value = await api.cancelTask(task.value.taskId)
    stopPolling()
    await loadReviewSurface()
  } catch (error) {
    presentError(error)
  } finally {
    busy.value = false
  }
}

async function retryTask() {
  busy.value = true
  try {
    task.value = await api.retryTask(task.value.taskId)
    schedulePoll()
  } catch (error) {
    presentError(error)
  } finally {
    busy.value = false
  }
}

onMounted(checkBackend)
onBeforeUnmount(stopPolling)
</script>

<template>
  <section class="agent-drawer no-print" aria-label="AI 简历优化">
    <header class="agent-drawer__header">
      <div>
        <p class="agent-drawer__eyebrow">AI OPTIMIZATION</p>
        <h2 class="title-font">AI 简历优化</h2>
        <p>基于当前岗位 JD 生成建议，审核后再合并到简历。</p>
      </div>
      <button type="button" class="agent-drawer__close" aria-label="关闭 AI 优化" @click="emit('close')">×</button>
    </header>

    <div class="agent-drawer__status">
      <span
        class="h-2 w-2 rounded-full"
        :class="backendStatus === 'online' ? 'bg-emerald-500' : backendStatus === 'checking' ? 'bg-amber-400' : 'bg-rose-500'"
      />
      {{ backendStatus === 'online' ? '后端在线' : backendStatus === 'checking' ? '正在检查服务' : '后端离线' }}
    </div>

    <div class="agent-drawer__body">
      <div class="agent-drawer__jd-source">
        <button
          v-if="jobDescription"
          type="button"
          :class="useTemporaryJobDescription ? 'small-btn' : 'primary-btn'"
          :disabled="busy"
          @click="useTemporaryJobDescription = false"
        >
          使用档案 JD
        </button>
        <button
          type="button"
          :class="useTemporaryJobDescription ? 'primary-btn' : 'small-btn'"
          :disabled="busy"
          @click="useTemporaryJobDescription = true"
        >
          使用临时 JD
        </button>
      </div>
      <div class="grid gap-4 lg:grid-cols-[1fr_260px]">
        <label>
          <span class="field-label mb-2 block">目标职位描述（JD）</span>
          <textarea
            v-model="localJobDescription"
            class="field-input min-h-28 w-full"
            maxlength="20000"
            :disabled="!useTemporaryJobDescription"
            :placeholder="useTemporaryJobDescription ? '粘贴职位职责、任职要求与技能要求……' : '当前使用岗位档案中保存的 JD'"
          />
          <p class="mt-2 text-xs leading-5" :class="jobDescriptionLength < 80 ? 'text-amber-700' : 'text-sky-700'">
            {{ jobDescriptionHint }}
          </p>
        </label>
        <div class="rounded-xl border border-slate-200 bg-white p-4">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">任务状态</p>
          <p class="mt-2 text-base font-bold text-slate-800">{{ taskStatusLabel }}</p>
          <p v-if="task?.taskId" class="mt-1 break-all text-[11px] text-slate-400">{{ task.taskId }}</p>
          <div class="mt-4 flex flex-wrap gap-2">
            <button type="button" class="primary-btn" :disabled="busy" @click="startTask">
              {{ busy ? '处理中…' : '分析当前简历' }}
            </button>
            <button
              v-if="task && activeStatuses.has(task.status)"
              type="button"
              class="small-btn"
              :disabled="busy"
              @click="cancelTask"
            >
              取消任务
            </button>
            <button
              v-if="task?.status === 'FAILED' && task.failureRetryable"
              type="button"
              class="small-btn"
              :disabled="busy"
              @click="retryTask"
            >
              重试
            </button>
          </div>
        </div>
      </div>

      <p v-if="statusMessage" class="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
        {{ statusMessage }}
      </p>
      <p v-if="errorMessage" class="mt-4 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
        {{ errorMessage }}
      </p>

      <div v-if="patches.length" class="mt-5 space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="font-bold text-slate-900">待审核建议（{{ patches.length }}）</h3>
          <button v-if="canMerge" type="button" class="primary-btn" :disabled="busy" @click="mergeAccepted">
            合并已接受建议
          </button>
        </div>
        <article v-for="patch in patches" :key="patch.patchId" class="rounded-xl border border-slate-200 bg-white p-4">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <code class="text-xs font-semibold text-sky-700">{{ patch.path }}</code>
            <span class="rounded-full bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">
              {{ patch.reviewStatus }} · {{ patch.policyDecision }}
            </span>
            <div class="mt-2 flex flex-wrap gap-1.5 text-[11px] font-semibold">
              <span class="rounded-full bg-sky-50 px-2 py-1 text-sky-700">{{ intentLabels[patch.intent] || patch.intent }}</span>
              <span class="rounded-full bg-slate-100 px-2 py-1 text-slate-600">{{ confidenceLabel(patch.confidence) }}</span>
              <span v-for="reference in patch.jdRefs || []" :key="reference" class="rounded-full bg-violet-50 px-2 py-1 text-violet-700">JD · {{ reference }}</span>
            </div>
          </div>
          <div class="mt-3 grid gap-3 lg:grid-cols-2">
            <div class="rounded-lg border border-rose-100 bg-rose-50 p-3">
              <p class="mb-1 text-xs font-bold text-rose-600">修改前</p>
              <p class="whitespace-pre-wrap text-sm leading-6 text-slate-700">{{ patch.before }}</p>
            </div>
            <div class="rounded-lg border border-emerald-100 bg-emerald-50 p-3">
              <p class="mb-1 text-xs font-bold text-emerald-600">建议修改</p>
              <p class="whitespace-pre-wrap text-sm leading-6 text-slate-700">{{ patch.after }}</p>
            </div>
          </div>
          <div v-if="patch.evidence?.length" class="mt-3 rounded-lg bg-slate-50 p-3">
            <p class="text-xs font-bold text-slate-600">证据</p>
            <p v-for="evidence in patch.evidence" :key="evidence.artifactId" class="mt-1 text-xs leading-5 text-slate-500">
              {{ evidence.excerpt }}
            </p>
          </div>
          <div v-if="patch.actions?.length" class="mt-4 flex flex-wrap items-end gap-2">
            <button type="button" class="primary-btn" :disabled="busy" @click="decidePatch(patch, 'ACCEPTED')">接受</button>
            <button type="button" class="small-btn" :disabled="busy" @click="decidePatch(patch, 'REJECTED')">拒绝</button>
            <label v-if="patch.actions.includes('EDIT')" class="min-w-[260px] flex-1">
              <span class="mb-1 block text-xs font-semibold text-slate-500">人工编辑后重新核验</span>
              <textarea v-model="editDrafts[patch.patchId]" class="field-input min-h-20 w-full" />
            </label>
            <button
              v-if="patch.actions.includes('EDIT')"
              type="button"
              class="small-btn"
              :disabled="busy"
              @click="editPatch(patch)"
            >
              提交编辑
            </button>
          </div>
        </article>
      </div>

      <div v-if="gaps.length" class="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4">
        <h3 class="font-bold text-amber-800">证据覆盖缺口</h3>
        <p v-for="gap in gaps" :key="`${gap.patchId}-${gap.path}`" class="mt-2 text-sm text-amber-700">
          {{ gap.path || '未定位字段' }}：{{ gap.reason }}
        </p>
      </div>

      <details v-if="timeline.length" class="mt-5 rounded-xl border border-slate-200 bg-white p-4">
        <summary class="cursor-pointer text-sm font-bold text-slate-700">最近执行时间线（{{ timeline.length }}）</summary>
        <ol class="mt-3 space-y-2">
          <li v-for="item in timeline" :key="item.eventId" class="flex justify-between gap-3 text-xs text-slate-500">
            <span>{{ item.type }} · {{ item.stage }}</span>
            <time>{{ formatOccurredAt(item.occurredAt) }}</time>
          </li>
        </ol>
      </details>
    </div>
  </section>
</template>

<style scoped>
.agent-drawer { min-height: 100%; background: #fbfdff; color: #17243a; padding: 28px; } .agent-drawer__header { display: flex; justify-content: space-between; gap: 16px; border-bottom: 1px solid #e1ebf2; padding-bottom: 18px; } .agent-drawer__eyebrow { margin: 0 0 6px; color: #3782ad; font-size: 10px; font-weight: 800; letter-spacing: .18em; } h2 { margin: 0; font-size: 24px; } .agent-drawer__header p:not(.agent-drawer__eyebrow) { margin: 7px 0 0; color: #64748b; font-size: 13px; line-height: 1.6; } .agent-drawer__close { width: 34px; height: 34px; border-radius: 50%; color: #64748b; font-size: 24px; } .agent-drawer__close:hover { background: #edf6fb; } .agent-drawer__status { display: flex; align-items: center; gap: 7px; border-bottom: 1px solid #e1ebf2; padding: 12px 0; color: #597389; font-size: 12px; } .agent-drawer__body { padding-top: 20px; } .agent-drawer__jd-source { display: flex; gap: 7px; margin: 0 0 12px; }
</style>
