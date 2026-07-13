<script setup>
import { reactive, ref, watch } from 'vue'
import { Dialog } from 'tdesign-vue-next'
import ResumeEditor from './components/resume/ResumeEditor.vue'
import ResumePreview from './components/resume/ResumePreview.vue'
import ResumeToolbar from './components/resume/ResumeToolbar.vue'
import { useResumeBuilder } from './composables/useResumeBuilder'

const {
  resume,
  panels,
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
