<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { Dialog, Drawer } from 'tdesign-vue-next'
import ResumeEditor from './components/resume/ResumeEditor.vue'
import ResumePreview from './components/resume/ResumePreview.vue'
import ResumeToolbar from './components/resume/ResumeToolbar.vue'
import ResumeAssetLibrary from './components/resume/ResumeAssetLibrary.vue'
import AgentWorkspace from './components/agent/AgentWorkspace.vue'
import { useResumeBuilder } from './composables/useResumeBuilder'
import { normalizeResumeData } from './modules/resume/normalize'

const {
  resume,
  panels,
  resumeAssets,
  currentAssetId,
  currentResumeAsset,
  assetsLoading,
  photoUploadMessage,
  photoUploadError,
  educationLogoFeedback,
  jsonStatusMessage,
  jsonErrorMessage,
  actionStatusMessage,
  actionErrorMessage,
  exportWarningMessage,
  jsonInputRef,
  pageOverflow,
  pageHeight,
  brandStyle,
  togglePanel,
  expandAllPanels,
  collapseAllPanels,
  loadDemo,
  clearAll,
  createResumeAssetVersion,
  selectResumeAsset,
  duplicateResumeAsset,
  updateResumeAssetMetadata,
  archiveResumeAsset,
  restoreResumeAsset,
  removeResumeAsset,
  saveDraft,
  restoreDraft,
  exportPdf,
  exportJson,
  triggerJsonImport,
  handleJsonImport,
  addEducation,
  removeEducation,
  toggleEducationHidden,
  moveEducationUp,
  moveEducationDown,
  onEducationLogoChange,
  removeEducationLogo,
  addInternship,
  removeInternship,
  toggleInternshipHidden,
  moveInternshipUp,
  moveInternshipDown,
  onLogoChange,
  removeLogo,
  addResearchExperience,
  removeResearchExperience,
  toggleResearchExperienceHidden,
  moveResearchExperienceUp,
  moveResearchExperienceDown,
  addProject,
  removeProject,
  toggleProjectHidden,
  moveProjectUp,
  moveProjectDown,
  addStudentExperience,
  removeStudentExperience,
  toggleStudentExperienceHidden,
  moveStudentExperienceUp,
  moveStudentExperienceDown,
  addCustomImage,
  removeCustomImage,
  toggleCustomImageHidden,
  moveCustomImageUp,
  moveCustomImageDown,
  addAward,
  removeAward,
  toggleAwardHidden,
  moveAwardUp,
  moveAwardDown,
  addCertificate,
  removeCertificate,
  toggleCertificateHidden,
  moveCertificateUp,
  moveCertificateDown,
  updateLayoutOrder,
  onPhotoChange,
  removePhoto,
  onCustomImageChange,
  removeCustomImageFile,
  onPageOverflowChange,
} = useResumeBuilder()

const dismissedNotices = reactive({
  jsonStatus: false,
  actionStatus: false,
  jsonError: false,
  actionError: false,
  exportWarning: false,
  pageOverflow: false,
})
const clearConfirmVisible = ref(false)
const clearConfirmLoading = ref(false)
const assetDrawerVisible = ref(false)
const agentDrawerVisible = ref(false)
const currentAssetLabel = computed(() => {
  const asset = currentResumeAsset.value
  if (!asset) return '未选择岗位简历'
  return [asset.targetCompany, asset.targetRole].filter(Boolean).join(' · ') || asset.title || '未命名岗位简历'
})

async function createAsset(type) {
  await createResumeAssetVersion(type)
}

async function selectAsset(id) {
  await selectResumeAsset(id)
  assetDrawerVisible.value = false
}

function dismissNotice(type) {
  dismissedNotices[type] = true
}

function requestClearAll() {
  clearConfirmVisible.value = true
}

function closeClearConfirm() {
  if (clearConfirmLoading.value) return
  clearConfirmVisible.value = false
}

async function confirmClearAll() {
  clearConfirmLoading.value = true
  try {
    await clearAll()
    clearConfirmVisible.value = false
  } finally {
    clearConfirmLoading.value = false
  }
}

function applyMergedResume(mergedResume) {
  Object.assign(resume, normalizeResumeData(mergedResume))
  actionErrorMessage.value = ''
  actionStatusMessage.value = `AI 审核结果已合并并同步到编辑器（后端版本 v${mergedResume.version}）。`
}

watch(() => jsonStatusMessage.value, () => {
  dismissedNotices.jsonStatus = false
})

watch(() => actionStatusMessage.value, () => {
  dismissedNotices.actionStatus = false
})

watch(() => jsonErrorMessage.value, () => {
  dismissedNotices.jsonError = false
})

watch(() => actionErrorMessage.value, () => {
  dismissedNotices.actionError = false
})

watch(() => exportWarningMessage.value, () => {
  dismissedNotices.exportWarning = false
})

watch(() => `${pageOverflow.value}-${pageHeight.value}`, () => {
  dismissedNotices.pageOverflow = false
})
</script>

<template>
  <div class="app-shell min-h-screen bg-white text-slate-700" :style="brandStyle">
    <main class="app-main mx-auto w-full max-w-[1480px] px-4 py-6 lg:px-8 lg:py-8">
      <ResumeToolbar
        @load-demo="loadDemo"
        @clear-all="requestClearAll"
        @save-draft="saveDraft"
        @restore-draft="restoreDraft"
        @export-json="exportJson"
        @import-json="triggerJsonImport"
        @export-pdf="exportPdf"
        @expand-all-panels="expandAllPanels"
        @collapse-all-panels="collapseAllPanels"
      />
      <section class="asset-hub no-print" aria-label="当前岗位简历">
        <div class="asset-hub__context">
          <span class="asset-hub__eyebrow">当前简历</span>
          <strong>{{ currentAssetLabel }}</strong>
          <small>{{ resumeAssets.length }} 份档案</small>
        </div>
        <div class="asset-hub__actions">
          <button type="button" class="asset-hub__manage" @click="assetDrawerVisible = true">管理版本</button>
          <button type="button" class="asset-hub__quiet" @click="agentDrawerVisible = true">AI 优化 <span aria-hidden="true">→</span></button>
        </div>
      </section>

      <Drawer v-model:visible="assetDrawerVisible" class="asset-library-drawer" placement="right" size="680px" :header="false" :footer="false">
        <ResumeAssetLibrary
          :assets="resumeAssets"
          :current-asset-id="currentAssetId"
          :loading="assetsLoading"
          @create="createAsset"
          @select="selectAsset"
          @duplicate="duplicateResumeAsset"
          @archive="archiveResumeAsset"
          @restore="restoreResumeAsset"
          @delete="removeResumeAsset"
          @update-metadata="updateResumeAssetMetadata"
          @close="assetDrawerVisible = false"
        />
      </Drawer>

      <Drawer v-model:visible="agentDrawerVisible" class="agent-workspace-drawer" placement="right" size="680px" :header="false" :footer="false">
        <AgentWorkspace
          :resume="resume"
          :job-description="currentResumeAsset?.jobDescription || ''"
          :asset-label="currentAssetLabel"
          @apply-merged-resume="applyMergedResume"
          @close="agentDrawerVisible = false"
        />
      </Drawer>

      <Dialog
        v-model:visible="clearConfirmVisible"
        header="确认清空当前内容"
        placement="center"
        width="480px"
        destroy-on-close
        :close-on-overlay-click="!clearConfirmLoading"
        :close-on-esc-keydown="!clearConfirmLoading"
        :confirm-btn="{ content: '确认清空', theme: 'danger', loading: clearConfirmLoading }"
        :cancel-btn="{ content: '取消', disabled: clearConfirmLoading }"
        @confirm="confirmClearAll"
        @close="closeClearConfirm"
        @cancel="closeClearConfirm"
      >
        <div class="space-y-2 text-[15px] leading-7 text-slate-700">
          <p>这会清空当前简历内容，并删除本地草稿。</p>
          <p class="text-slate-500">如果有还没导出的修改，清空后将无法恢复。</p>
        </div>
      </Dialog>

      <input
        ref="jsonInputRef"
        type="file"
        accept=".json,application/json"
        class="hidden"
        @change="handleJsonImport"
      />

      <section class="no-print mb-4 space-y-2">
        <div
          v-if="jsonStatusMessage && !dismissedNotices.jsonStatus"
          class="notice-banner border-emerald-200 bg-emerald-50 text-emerald-700"
        >
          <span class="notice-banner__text">{{ jsonStatusMessage }}</span>
          <button
            type="button"
            class="notice-banner__close"
            aria-label="关闭提示"
            @click="dismissNotice('jsonStatus')"
          >
            ×
          </button>
        </div>
        <div
          v-if="actionStatusMessage && !dismissedNotices.actionStatus"
          class="notice-banner border-emerald-200 bg-emerald-50 text-emerald-700"
        >
          <span class="notice-banner__text">{{ actionStatusMessage }}</span>
          <button
            type="button"
            class="notice-banner__close"
            aria-label="关闭提示"
            @click="dismissNotice('actionStatus')"
          >
            ×
          </button>
        </div>
        <div
          v-if="jsonErrorMessage && !dismissedNotices.jsonError"
          class="notice-banner border-rose-200 bg-rose-50 text-rose-700"
        >
          <span class="notice-banner__text">{{ jsonErrorMessage }}</span>
          <button
            type="button"
            class="notice-banner__close"
            aria-label="关闭提示"
            @click="dismissNotice('jsonError')"
          >
            ×
          </button>
        </div>
        <div
          v-if="actionErrorMessage && !dismissedNotices.actionError"
          class="notice-banner border-rose-200 bg-rose-50 text-rose-700"
        >
          <span class="notice-banner__text">{{ actionErrorMessage }}</span>
          <button
            type="button"
            class="notice-banner__close"
            aria-label="关闭提示"
            @click="dismissNotice('actionError')"
          >
            ×
          </button>
        </div>
        <div
          v-if="exportWarningMessage && !dismissedNotices.exportWarning"
          class="notice-banner border-amber-200 bg-amber-50 text-amber-700"
        >
          <span class="notice-banner__text">{{ exportWarningMessage }}</span>
          <button
            type="button"
            class="notice-banner__close"
            aria-label="关闭提示"
            @click="dismissNotice('exportWarning')"
          >
            ×
          </button>
        </div>
        <div
          v-if="pageOverflow && !dismissedNotices.pageOverflow"
          class="notice-banner border-amber-200 bg-amber-50 text-amber-700"
        >
          <span class="notice-banner__text">当前内容已超过一页 A4，高度约 {{ pageHeight }}px，导出时可能分页。</span>
          <button
            type="button"
            class="notice-banner__close"
            aria-label="关闭提示"
            @click="dismissNotice('pageOverflow')"
          >
            ×
          </button>
        </div>
      </section>

      <section class="app-layout grid gap-5 xl:grid-cols-[560px_1fr]">
        <ResumeEditor
          :resume="resume"
          :panels="panels"
          :photo-upload-message="photoUploadMessage"
          :photo-upload-error="photoUploadError"
          :education-logo-feedback="educationLogoFeedback"
          @update-layout-order="updateLayoutOrder"
          @toggle-panel="togglePanel"
          @photo-change="onPhotoChange"
          @remove-photo="removePhoto"
          @add-education="addEducation"
          @remove-education="removeEducation"
          @toggle-education-hidden="toggleEducationHidden"
          @move-education-up="moveEducationUp"
          @move-education-down="moveEducationDown"
          @education-logo-change="onEducationLogoChange"
          @remove-education-logo="removeEducationLogo"
          @add-internship="addInternship"
          @remove-internship="removeInternship"
          @toggle-internship-hidden="toggleInternshipHidden"
          @move-internship-up="moveInternshipUp"
          @move-internship-down="moveInternshipDown"
          @logo-change="onLogoChange"
          @remove-logo="removeLogo"
          @add-research-experience="addResearchExperience"
          @remove-research-experience="removeResearchExperience"
          @toggle-research-experience-hidden="toggleResearchExperienceHidden"
          @move-research-experience-up="moveResearchExperienceUp"
          @move-research-experience-down="moveResearchExperienceDown"
          @add-project="addProject"
          @remove-project="removeProject"
          @toggle-project-hidden="toggleProjectHidden"
          @move-project-up="moveProjectUp"
          @move-project-down="moveProjectDown"
          @add-student-experience="addStudentExperience"
          @remove-student-experience="removeStudentExperience"
          @toggle-student-experience-hidden="toggleStudentExperienceHidden"
          @move-student-experience-up="moveStudentExperienceUp"
          @move-student-experience-down="moveStudentExperienceDown"
          @add-custom-image="addCustomImage"
          @remove-custom-image="removeCustomImage"
          @toggle-custom-image-hidden="toggleCustomImageHidden"
          @move-custom-image-up="moveCustomImageUp"
          @move-custom-image-down="moveCustomImageDown"
          @custom-image-change="onCustomImageChange"
          @remove-custom-image-file="removeCustomImageFile"
          @add-award="addAward"
          @remove-award="removeAward"
          @toggle-award-hidden="toggleAwardHidden"
          @move-award-up="moveAwardUp"
          @move-award-down="moveAwardDown"
          @add-certificate="addCertificate"
          @remove-certificate="removeCertificate"
          @toggle-certificate-hidden="toggleCertificateHidden"
          @move-certificate-up="moveCertificateUp"
          @move-certificate-down="moveCertificateDown"
        />
        <ResumePreview :resume="resume" @page-overflow-change="onPageOverflowChange" />
      </section>
    </main>
  </div>
</template>
<style scoped>
.asset-hub { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; border-bottom: 1px solid #dce8ef; padding: 0 2px 13px; }
.asset-hub__context { display: flex; min-width: 0; align-items: baseline; gap: 9px; } .asset-hub__eyebrow { color: #71879a; font-size: 11px; font-weight: 700; } .asset-hub__context strong { overflow: hidden; color: #25445a; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; } .asset-hub__context small { color: #94a6b4; font-size: 11px; }
.asset-hub__actions { display: flex; flex-wrap: wrap; gap: 6px; } .asset-hub__manage, .asset-hub__quiet { border-radius: 7px; padding: 7px 10px; font-size: 12px; font-weight: 700; transition: .15s ease; } .asset-hub__manage { border: 1px solid #b8d9eb; background: #f3faff; color: #0873a4; } .asset-hub__manage:hover { background: #e5f6ff; } .asset-hub__quiet { border: 1px solid transparent; background: transparent; color: #58758b; } .asset-hub__quiet:hover { background: #f1f6f9; color: #145d83; }
@media (max-width: 640px) { .asset-hub { align-items: flex-start; flex-direction: column; } .asset-hub__context { flex-wrap: wrap; } }
</style>
