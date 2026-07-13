<script setup>
import { computed } from 'vue'
import Draggable from 'vuedraggable'
import { ColorPicker, Slider } from 'tdesign-vue-next'
import { BrowseIcon, BrowseOffIcon } from 'tdesign-icons-vue-next'
import {
  PHOTO_RATIO_OPTIONS,
  PHOTO_SIZE_MAX,
  PHOTO_SIZE_MIN,
  clampPhotoSize,
  getPhotoHeightByRatio,
  normalizePhotoConfig,
} from '../../modules/resume/photoConfig'
import { POLITICAL_AFFILIATION_DEFAULT, POLITICAL_AFFILIATION_OPTIONS } from '../../modules/resume/constants'
import { NAME_FONT_OPTIONS } from '../../modules/resume/nameFont'
import {
  LINE_HEIGHT_MAX,
  LINE_HEIGHT_MIN,
  SECTION_GAP_MAX,
  SECTION_GAP_MIN,
  EDUCATION_LOGO_SIZE_MAX,
  EDUCATION_LOGO_SIZE_MIN,
  clampLineHeight,
  clampSectionGap,
  clampEducationLogoSize,
  CONTENT_FONT_SIZE_MAX,
  CONTENT_FONT_SIZE_MIN,
  EMPHASIS_FONT_SIZE_MAX,
  EMPHASIS_FONT_SIZE_MIN,
  META_FONT_SIZE_MAX,
  META_FONT_SIZE_MIN,
  clampAwardMetaFontSize,
  clampAwardDescriptionFontSize,
  clampAwardTitleFontSize,
  clampCertificateMetaFontSize,
  clampCertificateDescriptionFontSize,
  clampCertificateTitleFontSize,
  clampInternshipCompanyMetaFontSize,
  clampInternshipHighlightsFontSize,
  clampInternshipRoleFontSize,
  clampInternshipSummaryFontSize,
  clampInternshipTimeFontSize,
  NAME_FONT_SIZE_MAX,
  NAME_FONT_SIZE_MIN,
  clampProjectMetaFontSize,
  clampProjectHighlightsFontSize,
  clampProjectNameFontSize,
  clampProjectSummaryFontSize,
  clampProjectTagFontSize,
  clampStudentHighlightsFontSize,
  clampStudentMetaFontSize,
  clampStudentNameFontSize,
  clampStudentSummaryFontSize,
  SCHOOL_FONT_SIZE_MAX,
  SCHOOL_FONT_SIZE_MIN,
  clampNameFontSize,
  clampSchoolFontSize,
  clampSelfSummaryFontSize,
  clampSkillsFontSize,
} from '../../modules/resume/typography'
import { createPersonalDetailItem } from '../../modules/resume/factories'
import { ORDERABLE_SECTION_LABELS, normalizeLayoutOrder } from '../../modules/resume/sections'

const props = defineProps({
  resume: { type: Object, required: true },
  panels: { type: Object, required: true },
  photoUploadMessage: { type: String, default: '' },
  photoUploadError: { type: String, default: '' },
  educationLogoFeedback: {
    type: Object,
    default: () => ({
      id: '',
      message: '',
      error: '',
    }),
  },
})

const emit = defineEmits([
  'update-layout-order',
  'toggle-panel',
  'photo-change',
  'remove-photo',
  'add-education',
  'remove-education',
  'toggle-education-hidden',
  'move-education-up',
  'move-education-down',
  'education-logo-change',
  'remove-education-logo',
  'add-internship',
  'remove-internship',
  'toggle-internship-hidden',
  'move-internship-up',
  'move-internship-down',
  'logo-change',
  'remove-logo',
  'add-project',
  'remove-project',
  'toggle-project-hidden',
  'move-project-up',
  'move-project-down',
  'add-student-experience',
  'remove-student-experience',
  'toggle-student-experience-hidden',
  'move-student-experience-up',
  'move-student-experience-down',
  'add-custom-image',
  'remove-custom-image',
  'toggle-custom-image-hidden',
  'move-custom-image-up',
  'move-custom-image-down',
  'custom-image-change',
  'remove-custom-image-file',
  'add-research-experience',
  'remove-research-experience',
  'toggle-research-experience-hidden',
  'move-research-experience-up',
  'move-research-experience-down',
  'add-award',
  'remove-award',
  'toggle-award-hidden',
  'move-award-up',
  'move-award-down',
  'add-certificate',
  'remove-certificate',
  'toggle-certificate-hidden',
  'move-certificate-up',
  'move-certificate-down',
])

/**
 * v-auto-resize — textarea 内容变化时自动撑高
 * 设置 height = scrollHeight，保证编辑框始终可见全部内容。
 */
const vAutoResize = {
  mounted(el) {
    const fit = () => {
      el.style.height = 'auto'
      el.style.height = el.scrollHeight + 'px'
    }
    el.__autoResizeFit = fit
    el.addEventListener('input', fit)
    requestAnimationFrame(fit)
  },
  updated(el) {
    requestAnimationFrame(() => {
      el.style.height = 'auto'
      el.style.height = el.scrollHeight + 'px'
    })
  },
  unmounted(el) {
    if (el.__autoResizeFit) {
      el.removeEventListener('input', el.__autoResizeFit)
      delete el.__autoResizeFit
    }
  },
}

const photoMetaSummary = computed(() => {
  const meta = props.resume.profile.photoMeta
  if (!meta) return ''

  const finalFormat = meta.finalFormat || 'JPG'
  if (meta.compressed === false && finalFormat === meta.originalFormat) {
    return `原图 ${meta.originalFormat} ${meta.originalSizeKB}KB（未压缩，${meta.width}×${meta.height}）`
  }

  return `原图 ${meta.originalFormat} ${meta.originalSizeKB}KB -> ${finalFormat} ${meta.finalSizeKB}KB（${meta.width}×${meta.height}）`
})

function getColorValue(value, fallback = '#4a9fff') {
  if (typeof value === 'string' && value.trim()) return value
  return fallback
}

props.resume.theme.photoConfig = normalizePhotoConfig(props.resume.theme?.photoConfig || {})
if (!Array.isArray(props.resume.profile.personalDetails)) {
  props.resume.profile.personalDetails = []
}

const photoRatioOptions = PHOTO_RATIO_OPTIONS
const nameFontOptions = NAME_FONT_OPTIONS
const politicalAffiliationOptions = POLITICAL_AFFILIATION_OPTIONS
const politicalAffiliationDefault = POLITICAL_AFFILIATION_DEFAULT

if (!Object.prototype.hasOwnProperty.call(props.resume.profile, 'politicalAffiliation')) {
  props.resume.profile.politicalAffiliation = politicalAffiliationDefault
}
if (!Object.prototype.hasOwnProperty.call(props.resume.profile, 'showPoliticalAffiliation')) {
  props.resume.profile.showPoliticalAffiliation = false
}
const photoSizeMin = PHOTO_SIZE_MIN
const photoSizeMax = PHOTO_SIZE_MAX
const nameFontSizeMin = NAME_FONT_SIZE_MIN
const nameFontSizeMax = NAME_FONT_SIZE_MAX
const schoolFontSizeMin = SCHOOL_FONT_SIZE_MIN
const schoolFontSizeMax = SCHOOL_FONT_SIZE_MAX
const contentFontSizeMin = CONTENT_FONT_SIZE_MIN
const contentFontSizeMax = CONTENT_FONT_SIZE_MAX
const lineHeightMin = LINE_HEIGHT_MIN
const lineHeightMax = LINE_HEIGHT_MAX
const lineHeightStep = 0.1
const sectionGapMin = SECTION_GAP_MIN
const sectionGapMax = SECTION_GAP_MAX
const educationLogoSizeMin = EDUCATION_LOGO_SIZE_MIN
const educationLogoSizeMax = EDUCATION_LOGO_SIZE_MAX
const metaFontSizeMin = META_FONT_SIZE_MIN
const metaFontSizeMax = META_FONT_SIZE_MAX
const emphasisFontSizeMin = EMPHASIS_FONT_SIZE_MIN
const emphasisFontSizeMax = EMPHASIS_FONT_SIZE_MAX
const internshipHeaderFontSizeMin = Math.max(metaFontSizeMin, emphasisFontSizeMin)
const internshipHeaderFontSizeMax = Math.min(metaFontSizeMax, emphasisFontSizeMax)
const photoConfig = computed(() => props.resume.theme.photoConfig)
const sliderInputProps = {
  theme: 'column',
  size: 'small',
}
const orderableSections = computed({
  get() {
    return normalizeLayoutOrder(props.resume.layout?.order).map((id) => ({
      id,
      label: ORDERABLE_SECTION_LABELS[id] || id,
    }))
  },
  set(value) {
    emit(
      'update-layout-order',
      value.map((item) => item.id)
    )
  },
})
const editorSectionOrderMap = computed(() => {
  const map = {}
  normalizeLayoutOrder(props.resume.layout?.order).forEach((id, index) => {
    map[id] = index + 10
  })
  return map
})

function getEditorSectionOrder(id, fallback = 99) {
  return editorSectionOrderMap.value[id] ?? fallback
}

function updatePhotoConfig(patch = {}) {
  props.resume.theme.photoConfig = normalizePhotoConfig({
    ...photoConfig.value,
    ...patch,
  })
}

function onPhotoRatioChange(value) {
  updatePhotoConfig({ ratio: value })
}

function onPhotoLockChange(value) {
  updatePhotoConfig({ lockRatio: Boolean(value) })
}

function onPhotoWidthChange(value) {
  const width = clampPhotoSize(value, photoConfig.value.width)
  if (photoConfig.value.lockRatio) {
    updatePhotoConfig({
      width,
      height: getPhotoHeightByRatio(width, photoConfig.value.ratio),
    })
    return
  }
  updatePhotoConfig({ width })
}

function onPhotoHeightChange(value) {
  if (photoConfig.value.lockRatio) return
  updatePhotoConfig({
    height: clampPhotoSize(value, photoConfig.value.height),
  })
}

function onNameFontSizeChange(value) {
  props.resume.theme.nameFontSize = clampNameFontSize(value, props.resume.theme.nameFontSize)
}

function onSchoolFontSizeChange(value) {
  props.resume.theme.schoolFontSize = clampSchoolFontSize(value, props.resume.theme.schoolFontSize)
}

function onSkillsFontSizeChange(value) {
  props.resume.theme.skillsFontSize = clampSkillsFontSize(value, props.resume.theme.skillsFontSize)
}

function onInternshipSummaryFontSizeChange(value) {
  props.resume.theme.internshipSummaryFontSize = clampInternshipSummaryFontSize(
    value,
    props.resume.theme.internshipSummaryFontSize
  )
}

function onInternshipHighlightsFontSizeChange(value) {
  props.resume.theme.internshipHighlightsFontSize = clampInternshipHighlightsFontSize(
    value,
    props.resume.theme.internshipHighlightsFontSize
  )
}

function getInternshipHeaderFontSize() {
  const values = [
    clampInternshipTimeFontSize(props.resume.theme.internshipTimeFontSize),
    clampInternshipCompanyMetaFontSize(props.resume.theme.internshipCompanyMetaFontSize),
    clampInternshipRoleFontSize(props.resume.theme.internshipRoleFontSize),
  ]
  const average = values.reduce((sum, current) => sum + current, 0) / values.length
  return Math.min(
    internshipHeaderFontSizeMax,
    Math.max(internshipHeaderFontSizeMin, Math.round(average * 10) / 10)
  )
}

function onInternshipHeaderFontSizeChange(value) {
  const nextValue = Math.min(
    internshipHeaderFontSizeMax,
    Math.max(internshipHeaderFontSizeMin, Math.round(Number(value) * 10) / 10)
  )
  props.resume.theme.internshipTimeFontSize = clampInternshipTimeFontSize(
    nextValue,
    props.resume.theme.internshipTimeFontSize
  )
  props.resume.theme.internshipCompanyMetaFontSize = clampInternshipCompanyMetaFontSize(
    nextValue,
    props.resume.theme.internshipCompanyMetaFontSize
  )
  props.resume.theme.internshipRoleFontSize = clampInternshipRoleFontSize(
    nextValue,
    props.resume.theme.internshipRoleFontSize
  )
}

function onProjectSummaryFontSizeChange(value) {
  props.resume.theme.projectSummaryFontSize = clampProjectSummaryFontSize(
    value,
    props.resume.theme.projectSummaryFontSize
  )
}

function onProjectHighlightsFontSizeChange(value) {
  props.resume.theme.projectHighlightsFontSize = clampProjectHighlightsFontSize(
    value,
    props.resume.theme.projectHighlightsFontSize
  )
}

function onProjectNameFontSizeChange(value) {
  props.resume.theme.projectNameFontSize = clampProjectNameFontSize(
    value,
    props.resume.theme.projectNameFontSize
  )
}

function onProjectMetaFontSizeChange(value) {
  props.resume.theme.projectMetaFontSize = clampProjectMetaFontSize(
    value,
    props.resume.theme.projectMetaFontSize
  )
}

function onProjectTagFontSizeChange(value) {
  props.resume.theme.projectTagFontSize = clampProjectTagFontSize(
    value,
    props.resume.theme.projectTagFontSize
  )
}

function onStudentNameFontSizeChange(value) {
  props.resume.theme.studentNameFontSize = clampStudentNameFontSize(value, props.resume.theme.studentNameFontSize)
}

function onStudentMetaFontSizeChange(value) {
  props.resume.theme.studentMetaFontSize = clampStudentMetaFontSize(value, props.resume.theme.studentMetaFontSize)
}

function onStudentSummaryFontSizeChange(value) {
  props.resume.theme.studentSummaryFontSize = clampStudentSummaryFontSize(value, props.resume.theme.studentSummaryFontSize)
}

function onStudentHighlightsFontSizeChange(value) {
  props.resume.theme.studentHighlightsFontSize = clampStudentHighlightsFontSize(value, props.resume.theme.studentHighlightsFontSize)
}

function onAwardTitleFontSizeChange(value) {
  props.resume.theme.awardTitleFontSize = clampAwardTitleFontSize(
    value,
    props.resume.theme.awardTitleFontSize
  )
}

function onAwardMetaFontSizeChange(value) {
  props.resume.theme.awardMetaFontSize = clampAwardMetaFontSize(
    value,
    props.resume.theme.awardMetaFontSize
  )
}

function onAwardDescriptionFontSizeChange(value) {
  props.resume.theme.awardDescriptionFontSize = clampAwardDescriptionFontSize(
    value,
    props.resume.theme.awardDescriptionFontSize
  )
}

function onCertificateTitleFontSizeChange(value) {
  props.resume.theme.certificateTitleFontSize = clampCertificateTitleFontSize(
    value,
    props.resume.theme.certificateTitleFontSize
  )
}

function onCertificateMetaFontSizeChange(value) {
  props.resume.theme.certificateMetaFontSize = clampCertificateMetaFontSize(
    value,
    props.resume.theme.certificateMetaFontSize
  )
}

function onCertificateDescriptionFontSizeChange(value) {
  props.resume.theme.certificateDescriptionFontSize = clampCertificateDescriptionFontSize(
    value,
    props.resume.theme.certificateDescriptionFontSize
  )
}

function onSelfSummaryFontSizeChange(value) {
  props.resume.theme.selfSummaryFontSize = clampSelfSummaryFontSize(
    value,
    props.resume.theme.selfSummaryFontSize
  )
}

function addPersonalDetail() {
  props.resume.profile.personalDetails.push(createPersonalDetailItem())
}

function removePersonalDetail(id) {
  const index = props.resume.profile.personalDetails.findIndex((item) => item.id === id)
  if (index >= 0) props.resume.profile.personalDetails.splice(index, 1)
}

function movePersonalDetail(index, offset) {
  const list = props.resume.profile.personalDetails
  const nextIndex = index + offset
  if (index < 0 || nextIndex < 0 || index >= list.length || nextIndex >= list.length) return
  const [target] = list.splice(index, 1)
  list.splice(nextIndex, 0, target)
}
</script>

<template>
  <aside class="no-print flex flex-col gap-4 xl:sticky xl:top-5 xl:h-[calc(100vh-5rem)] xl:overflow-auto">
    <article class="panel-card" :style="{ order: 100 }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'layout')">
          <span class="panel-caret">{{ panels.layout ? '▾' : '▸' }}</span>
          <span class="panel-title">模块顺序管理</span>
        </button>
      </div>
      <div v-if="panels.layout" class="panel-body mt-4 space-y-3">
        <p class="text-sm leading-6 text-slate-500">
          基本信息和教育背景固定在顶部，下面这些栏目可以自由拖拽排序。
        </p>
        <draggable
          v-model="orderableSections"
          item-key="id"
          handle=".module-drag-handle"
          class="space-y-2"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
          :animation="180"
        >
          <template #item="{ element: item, index }">
            <div class="module-order-row flex items-center justify-between gap-3">
              <div class="flex min-w-0 items-center gap-3">
                <span class="text-xs font-semibold text-slate-400">{{ index + 1 }}</span>
                <span class="truncate text-sm font-medium text-slate-700">{{ item.label }}</span>
              </div>
              <button type="button" class="module-drag-handle" title="拖拽排序">::</button>
            </div>
          </template>
        </draggable>
      </div>
    </article>

    <article class="panel-card" :style="{ order: 1 }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'profile')">
          <span class="panel-caret">{{ panels.profile ? '▾' : '▸' }}</span>
          <span class="panel-title">基本信息</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.profile ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.profile = !resume.sectionVisibility.profile"
        >
          <browse-icon
            v-if="resume.sectionVisibility.profile"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
      <div v-if="panels.profile" class="panel-body mt-4 grid gap-3 md:grid-cols-3">
        <label class="field-wrap">
          <span class="field-label">姓名</span>
          <input v-model="resume.profile.name" class="field-input" placeholder="请输入姓名" />
        </label>
        <label class="field-wrap">
          <span class="field-label">邮箱</span>
          <input v-model="resume.profile.email" class="field-input" placeholder="请输入邮箱" />
        </label>
        <div class="field-wrap md:col-span-3">
          <div class="flex flex-wrap items-end gap-4">
            <label class="field-wrap min-w-[160px]">
              <span class="field-label">政治面貌</span>
              <select v-model="resume.profile.politicalAffiliation" class="field-input">
                <option
                  v-for="opt in politicalAffiliationOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </option>
              </select>
            </label>
            <label class="switch-field mb-0.5">
              <input
                type="checkbox"
                class="h-4 w-4 accent-sky-600"
                v-model="resume.profile.showPoliticalAffiliation"
              />
              <span>在简历中显示</span>
            </label>
          </div>
        </div>
        <label class="field-wrap">
          <span class="field-label">联系方式</span>
          <input v-model="resume.profile.phone" class="field-input" placeholder="请输入电话号" />
        </label>
        <div class="field-wrap md:col-span-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <div class="flex items-center justify-between gap-3">
            <div>
              <span class="field-label block">个人信息</span>
              <p class="mt-1 text-xs text-slate-500">可添加 CET-6、GPA、排名、求职城市等头部信息。</p>
            </div>
            <button type="button" class="small-btn" @click="addPersonalDetail">+ 新增</button>
          </div>
          <div v-if="resume.profile.personalDetails.length" class="mt-3 space-y-2">
            <div
              v-for="(item, index) in resume.profile.personalDetails"
              :key="item.id"
              class="rounded-lg border border-slate-200 bg-white px-3 py-3"
            >
              <div class="grid gap-3 sm:grid-cols-[132px_1fr_auto]">
                <label class="field-wrap">
                  <span class="field-label">标签</span>
                  <input v-model="item.label" class="field-input" placeholder="例如：GPA" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">内容</span>
                  <input v-model="item.value" class="field-input" placeholder="例如：3.83/4.0" />
                </label>
                <div class="flex items-end gap-1.5">
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === 0"
                    @click="movePersonalDetail(index, -1)"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === resume.profile.personalDetails.length - 1"
                    @click="movePersonalDetail(index, 1)"
                  >
                    下移
                  </button>
                  <button type="button" class="small-btn small-btn-danger" @click="removePersonalDetail(item.id)">
                    删除
                  </button>
                </div>
              </div>
            </div>
          </div>
          <p v-else class="mt-3 rounded-lg border border-dashed border-slate-300 px-3 py-3 text-sm text-slate-500">
            还没有个人信息项，点击右上角“新增”即可添加。
          </p>
        </div>
        <label class="field-wrap md:col-span-3">
          <span class="field-label">个人网站</span>
          <input
            v-model="resume.profile.website"
            class="field-input"
            placeholder="请输入个人网站,支持github、gitee图标自动解析"
          />
        </label>
        <label class="field-wrap md:col-span-3">
          <span class="field-label">求职意向</span>
          <input v-model="resume.profile.title" class="field-input" placeholder="不填写则不展示" />
        </label>
        <label class="field-wrap md:col-span-3">
          <span class="field-label">证件照上传（JPG / PNG / WebP，5MB 内保留原图）</span>
          <div class="mt-2 flex flex-wrap items-center gap-3">
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              class="file-input"
              @change="$emit('photo-change', $event)"
            />
            <button type="button" class="danger-btn" @click="$emit('remove-photo')">移除证件照</button>
          </div>
          <p v-if="photoUploadMessage" class="mt-2 text-xs font-medium text-emerald-600">
            {{ photoUploadMessage }}
          </p>
          <p v-if="photoUploadError" class="mt-2 text-xs font-medium text-rose-600">
            {{ photoUploadError }}
          </p>
          <p v-if="photoMetaSummary" class="mt-1 text-xs text-slate-500">{{ photoMetaSummary }}</p>
        </label>
        <div class="field-wrap md:col-span-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <span class="field-label">证件照尺寸设置</span>
          <div class="mt-2 grid gap-3 sm:grid-cols-2">
            <label class="field-wrap">
              <span class="field-label">比例</span>
              <select
                class="field-input"
                :value="photoConfig.ratio"
                @change="onPhotoRatioChange($event.target.value)"
              >
                <option v-for="ratio in photoRatioOptions" :key="ratio" :value="ratio">
                  {{ ratio }}
                </option>
              </select>
            </label>
            <label class="switch-field mt-auto">
              <input
                type="checkbox"
                class="h-4 w-4 accent-sky-600"
                :checked="photoConfig.lockRatio"
                @change="onPhotoLockChange($event.target.checked)"
              />
              <span>锁定比例</span>
            </label>
            <label class="field-wrap">
              <span class="field-label">宽度（{{ photoSizeMin }}-{{ photoSizeMax }}）</span>
              <div class="setting-slider mt-1">
                <Slider
                  :model-value="photoConfig.width"
                  :min="photoSizeMin"
                  :max="photoSizeMax"
                  :input-number-props="sliderInputProps"
                  @change="onPhotoWidthChange"
                />
              </div>
            </label>
            <label class="field-wrap">
              <span class="field-label">高度（{{ photoSizeMin }}-{{ photoSizeMax }}）</span>
              <div class="setting-slider mt-1" :class="{ 'setting-slider-disabled': photoConfig.lockRatio }">
                <Slider
                  :model-value="photoConfig.height"
                  :min="photoSizeMin"
                  :max="photoSizeMax"
                  :disabled="photoConfig.lockRatio"
                  :input-number-props="sliderInputProps"
                  @change="onPhotoHeightChange"
                />
              </div>
            </label>
          </div>
          <p class="mt-2 text-xs text-slate-500">
            当前尺寸：{{ photoConfig.width }} × {{ photoConfig.height }} px
          </p>
        </div>
      </div>
    </article>

    <article class="panel-card" :style="{ order: 2 }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'education')">
          <span class="panel-caret">{{ panels.education ? '▾' : '▸' }}</span>
          <span class="panel-title">教育背景</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.education ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.education = !resume.sectionVisibility.education"
        >
          <browse-icon
            v-if="resume.sectionVisibility.education"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
      <div v-if="panels.education" class="panel-body mt-4 space-y-3">
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">学校 Logo 大小</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.educationLogoSize"
              :min="educationLogoSizeMin"
              :max="educationLogoSizeMax"
              :step="1"
              :input-number-props="sliderInputProps"
              @update:model-value="resume.theme.educationLogoSize = clampEducationLogoSize($event)"
            />
          </div>
        </div>
        <div
          v-if="!resume.educations.length"
          class="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600"
        >
          暂无教育经历，点击下方按钮新增。
        </div>
        <draggable
          v-else
          v-model="resume.educations"
          item-key="id"
          handle=".drag-handle"
          class="drag-list"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
          :animation="180"
        >
          <template #item="{ element: item, index }">
            <article class="sub-card drag-item">
              <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <button type="button" class="drag-handle" title="拖拽排序">::</button>
                  <span class="text-sm font-semibold text-slate-800">教育 {{ index + 1 }}</span>
                  <span
                    v-if="item.hidden"
                    class="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700"
                  >
                    已隐藏（预览不显示）
                  </span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                  <button type="button" class="small-btn" @click="$emit('toggle-education-hidden', item.id)">
                    {{ item.hidden ? '显示' : '隐藏' }}
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === 0"
                    @click="$emit('move-education-up', index)"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === resume.educations.length - 1"
                    @click="$emit('move-education-down', index)"
                  >
                    下移
                  </button>
                  <button type="button" class="small-btn small-btn-danger" @click="$emit('remove-education', item.id)">
                    删除
                  </button>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <label class="field-wrap">
                  <span class="field-label">学校</span>
                  <input v-model="item.school" class="field-input" placeholder="例如：北京大学(985)" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">学历</span>
                  <input v-model="item.degree" class="field-input" placeholder="例如：本科" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">专业</span>
                  <input v-model="item.major" class="field-input" placeholder="例如：网络工程" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">起止时间</span>
                  <input v-model="item.studyPeriod" class="field-input" placeholder="例如：2023-2027" />
                </label>
                <div class="field-wrap sm:col-span-2">
                  <span class="field-label">学校 Logo（JPG / PNG / WebP，≤2MB）</span>
                  <div class="mt-2 flex items-center gap-3">
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      class="file-input"
                      @change="$emit('education-logo-change', item.id, $event)"
                    />
                    <button
                      type="button"
                      class="toolbar-btn !px-3 !py-1.5 !text-xs"
                      @click="$emit('remove-education-logo', item.id)"
                    >
                      移除
                    </button>
                  </div>
                  <p
                    v-if="educationLogoFeedback?.id === item.id && educationLogoFeedback.message"
                    class="mt-1 text-xs font-medium text-emerald-600"
                  >
                    {{ educationLogoFeedback.message }}
                  </p>
                  <p
                    v-if="educationLogoFeedback?.id === item.id && educationLogoFeedback.error"
                    class="mt-1 text-xs font-medium text-rose-600"
                  >
                    {{ educationLogoFeedback.error }}
                  </p>
                </div>
              </div>
            </article>
          </template>
        </draggable>
        <button type="button" class="toolbar-btn w-full border-dashed" @click="$emit('add-education')">
          + 新增教育经历
        </button>
      </div>
    </article>

    <article class="panel-card" :style="{ order: getEditorSectionOrder('skills') }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'skills')">
          <span class="panel-caret">{{ panels.skills ? '▾' : '▸' }}</span>
          <span class="panel-title">技术栈</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.skills ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.skills = !resume.sectionVisibility.skills"
        >
          <browse-icon
            v-if="resume.sectionVisibility.skills"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
      <div v-if="panels.skills" class="panel-body mt-4 space-y-3">
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">正文字号</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.skillsFontSize"
              :min="contentFontSizeMin"
              :max="contentFontSizeMax"
              :step="0.5"
              :input-number-props="sliderInputProps"
              @change="onSkillsFontSizeChange"
            />
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">正文行距</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.skillsLineHeight"
              :min="lineHeightMin"
              :max="lineHeightMax"
              :step="lineHeightStep"
              :input-number-props="sliderInputProps"
              @update:model-value="resume.theme.skillsLineHeight = clampLineHeight($event)"
            />
          </div>
        </div>
        <label class="field-wrap">
          <span class="field-label">技能内容（支持 **加粗**）</span>
          <textarea v-model="resume.skills" v-auto-resize class="field-input field-textarea"></textarea>
        </label>
      </div>
    </article>

    <article class="panel-card" :style="{ order: getEditorSectionOrder('internships') }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'internship')">
          <span class="panel-caret">{{ panels.internship ? '▾' : '▸' }}</span>
          <span class="panel-title">实习经历</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.internships ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.internships = !resume.sectionVisibility.internships"
        >
          <browse-icon
            v-if="resume.sectionVisibility.internships"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
      <div v-if="panels.internship" class="panel-body mt-4 space-y-3">
          <div class="internship-font-grid grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">实习岗位信息字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="getInternshipHeaderFontSize()"
                  :min="internshipHeaderFontSizeMin"
                  :max="internshipHeaderFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @change="onInternshipHeaderFontSizeChange"
                />
              </div>
              <p class="mt-2 text-xs leading-5 text-slate-500">同步控制时间、公司信息和岗位</p>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">公司信息字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="resume.theme.internshipCompanyMetaFontSize"
                  :min="metaFontSizeMin"
                  :max="metaFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @update:model-value="resume.theme.internshipCompanyMetaFontSize = clampInternshipCompanyMetaFontSize($event)"
                />
              </div>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">岗位字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="resume.theme.internshipRoleFontSize"
                  :min="emphasisFontSizeMin"
                  :max="emphasisFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @update:model-value="resume.theme.internshipRoleFontSize = clampInternshipRoleFontSize($event)"
                />
              </div>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">简介字号</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.internshipSummaryFontSize"
                :min="contentFontSizeMin"
                :max="contentFontSizeMax"
                :step="0.5"
                :input-number-props="sliderInputProps"
                @change="onInternshipSummaryFontSizeChange"
              />
            </div>
          </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">简介行距</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.internshipSummaryLineHeight"
                :min="lineHeightMin"
                :max="lineHeightMax"
                :step="lineHeightStep"
                :input-number-props="sliderInputProps"
                @update:model-value="resume.theme.internshipSummaryLineHeight = clampLineHeight($event)"
              />
            </div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">亮点字号</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.internshipHighlightsFontSize"
                :min="contentFontSizeMin"
                :max="contentFontSizeMax"
                :step="0.5"
                :input-number-props="sliderInputProps"
                @change="onInternshipHighlightsFontSizeChange"
              />
            </div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">亮点行距</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.internshipHighlightsLineHeight"
                :min="lineHeightMin"
                :max="lineHeightMax"
                :step="lineHeightStep"
                :input-number-props="sliderInputProps"
                @update:model-value="resume.theme.internshipHighlightsLineHeight = clampLineHeight($event)"
              />
            </div>
          </div>
        </div>
        <div
          v-if="!resume.internships.length"
          class="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600"
        >
          暂无实习经历，点击下方按钮新增。
        </div>
        <draggable
          v-else
          v-model="resume.internships"
          item-key="id"
          handle=".drag-handle"
          class="drag-list"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
          :animation="180"
        >
          <template #item="{ element: item, index }">
            <article class="sub-card drag-item">
              <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <button type="button" class="drag-handle" title="拖拽排序">::</button>
                  <span class="text-sm font-semibold text-slate-800">实习 {{ index + 1 }}</span>
                  <span
                    v-if="item.hidden"
                    class="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700"
                  >
                    已隐藏（预览不显示）
                  </span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                  <button type="button" class="small-btn" @click="$emit('toggle-internship-hidden', item.id)">
                    {{ item.hidden ? '显示' : '隐藏' }}
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === 0"
                    @click="$emit('move-internship-up', index)"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === resume.internships.length - 1"
                    @click="$emit('move-internship-down', index)"
                  >
                    下移
                  </button>
                  <button type="button" class="small-btn small-btn-danger" @click="$emit('remove-internship', item.id)">
                    删除
                  </button>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <label class="field-wrap">
                  <span class="field-label">公司名称</span>
                  <input v-model="item.company" class="field-input" placeholder="例如：阿里巴巴" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">岗位</span>
                  <input v-model="item.role" class="field-input" placeholder="例如：后端开发实习生" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">业务线 / 部门</span>
                  <input v-model="item.department" class="field-input" placeholder="例如：技术部-后端组" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">时间</span>
                  <input v-model="item.period" class="field-input" placeholder="例如:2026.03 - 2026.08" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">地点（可留空）</span>
                  <input v-model="item.location" class="field-input" placeholder="例如：深圳" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">工作简介</span>
                  <textarea
                    v-model="item.summary"
                    v-auto-resize
                    class="field-input field-textarea"
                    placeholder="简要描述职责范围和业务背景"
                  ></textarea>
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">成果亮点（按行输入）</span>
                  <textarea
                    v-model="item.highlights"
                    v-auto-resize
                    class="field-input field-textarea"
                    placeholder="每行一条，可用 **关键字** 强调"
                  ></textarea>
                </label>
                <div class="field-wrap sm:col-span-2">
                  <span class="field-label">公司 Logo</span>
                  <div class="mt-2 flex items-center gap-3">
                    <input type="file" accept="image/*" class="file-input" @change="$emit('logo-change', item.id, $event)" />
                    <button type="button" class="toolbar-btn !px-3 !py-1.5 !text-xs" @click="$emit('remove-logo', item.id)">
                      移除
                    </button>
                  </div>
                </div>
                <div class="field-wrap">
                  <span class="field-label">条目背景色</span>
                  <div class="mt-2 flex items-center gap-2">
                    <div class="color-panel-wrap">
                      <ColorPicker
                        :model-value="getColorValue(item.stripColor, resume.theme.primaryColor || '#4a9fff')"
                        format="RGBA"
                        :enable-alpha="true"
                        :show-primary-color-preview="false"
                        :color-modes="['monochrome', 'linear-gradient']"
                        @update:model-value="item.stripColor = $event"
                        @change="item.stripColor = $event"
                      />
                    </div>
                    <button type="button" class="small-btn" @click="item.stripColor = ''">跟随主题</button>
                  </div>
                </div>
                <div class="field-wrap">
                  <span class="field-label">Logo 大小</span>
                  <div class="mt-2 flex items-center gap-2">
                    <input v-model.number="item.logoSize" type="range" min="14" max="48" class="w-full accent-sky-600" />
                    <input
                      v-model.number="item.logoSize"
                      type="number"
                      min="14"
                      max="48"
                      class="field-input !w-20 !px-2 !py-1.5 text-center"
                    />
                  </div>
                </div>
              </div>
            </article>
          </template>
        </draggable>
        <button type="button" class="toolbar-btn w-full border-dashed" @click="$emit('add-internship')">
          + 新增实习经历
        </button>
      </div>
    </article>

    <article class="panel-card" :style="{ order: getEditorSectionOrder('researchExperiences') }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'research')">
          <span class="panel-caret">{{ panels.research ? '▾' : '▸' }}</span>
          <span class="panel-title">科研经历</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.researchExperiences ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.researchExperiences = !resume.sectionVisibility.researchExperiences"
        >
          <browse-icon
            v-if="resume.sectionVisibility.researchExperiences"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
      <div v-if="panels.research" class="panel-body mt-4 space-y-3">
        <div
          v-if="!resume.researchExperiences.length"
          class="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600"
        >
          暂无科研经历，点击下方按钮新增。
        </div>
        <draggable
          v-else
          v-model="resume.researchExperiences"
          item-key="id"
          handle=".drag-handle"
          class="drag-list"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
          :animation="180"
        >
          <template #item="{ element: item, index }">
            <article class="sub-card drag-item">
              <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <button type="button" class="drag-handle" title="拖拽排序">::</button>
                  <span class="text-sm font-semibold text-slate-800">科研 {{ index + 1 }}</span>
                  <span
                    v-if="item.hidden"
                    class="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700"
                  >
                    已隐藏（预览不显示）
                  </span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                  <button type="button" class="small-btn" @click="$emit('toggle-research-experience-hidden', item.id)">
                    {{ item.hidden ? '显示' : '隐藏' }}
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === 0"
                    @click="$emit('move-research-experience-up', index)"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === resume.researchExperiences.length - 1"
                    @click="$emit('move-research-experience-down', index)"
                  >
                    下移
                  </button>
                  <button
                    type="button"
                    class="small-btn small-btn-danger"
                    @click="$emit('remove-research-experience', item.id)"
                  >
                    删除
                  </button>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">科研课题 / 方向</span>
                  <input v-model="item.title" class="field-input" placeholder="例如：具身智能导航中的多模态感知研究" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">实验室 / 课题组</span>
                  <input v-model="item.lab" class="field-input" placeholder="例如：智能感知与机器人实验室" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">角色 / 身份</span>
                  <input v-model="item.role" class="field-input" placeholder="例如：本科科研助理 / 一作" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">指导老师</span>
                  <input v-model="item.supervisor" class="field-input" placeholder="例如：李某某教授" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">时间</span>
                  <input v-model="item.period" class="field-input" placeholder="例如：2024.03 - 2025.01" />
                </label>
                <label class="switch-field sm:col-span-2">
                  <input v-model="item.hidePaperInfo" type="checkbox" class="h-4 w-4 accent-sky-600" />
                  <span>隐藏论文信息区域（适用于暂未发表/投稿的情况）</span>
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">论文标题 / 成果标题</span>
                  <input
                    v-model="item.paperTitle"
                    class="field-input"
                    placeholder="例如：Robust Object Recognition for Indoor Navigation"
                    :disabled="item.hidePaperInfo"
                  />
                </label>
                <label class="field-wrap">
                  <span class="field-label">期刊 / 会议 / 平台</span>
                  <input
                    v-model="item.journal"
                    class="field-input"
                    placeholder="例如：IEEE T-RO / 中文核心 / 预印本"
                    :disabled="item.hidePaperInfo"
                  />
                </label>
                <label class="field-wrap">
                  <span class="field-label">论文状态</span>
                  <input
                    v-model="item.publicationStatus"
                    class="field-input"
                    placeholder="例如：已录用 / 审稿中 / 已发表"
                    :disabled="item.hidePaperInfo"
                  />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">研究简介</span>
                  <textarea
                    v-model="item.summary"
                    class="field-input field-textarea h-24"
                    placeholder="简要说明研究问题、方法或你负责的部分"
                  ></textarea>
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">成果亮点（按行输入）</span>
                  <textarea
                    v-model="item.highlights"
                    class="field-input field-textarea h-28"
                    placeholder="每行一条，可写实验结果、论文贡献、投稿情况等"
                  ></textarea>
                </label>
              </div>
            </article>
          </template>
        </draggable>
        <button type="button" class="toolbar-btn w-full border-dashed" @click="$emit('add-research-experience')">
          + 新增科研经历
        </button>
      </div>
    </article>

    <article class="panel-card" :style="{ order: getEditorSectionOrder('projects') }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'project')">
          <span class="panel-caret">{{ panels.project ? '▾' : '▸' }}</span>
          <span class="panel-title">项目经历</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.projects ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.projects = !resume.sectionVisibility.projects"
        >
          <browse-icon
            v-if="resume.sectionVisibility.projects"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
      <div v-if="panels.project" class="panel-body mt-4 space-y-3">
          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">项目名称字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="resume.theme.projectNameFontSize"
                  :min="emphasisFontSizeMin"
                  :max="emphasisFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @change="onProjectNameFontSizeChange"
                />
              </div>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">角色时间字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="resume.theme.projectMetaFontSize"
                  :min="metaFontSizeMin"
                  :max="metaFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @change="onProjectMetaFontSizeChange"
                />
              </div>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">技术栈字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="resume.theme.projectTagFontSize"
                  :min="metaFontSizeMin"
                  :max="metaFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @change="onProjectTagFontSizeChange"
                />
              </div>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">简介字号</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.projectSummaryFontSize"
                :min="contentFontSizeMin"
                :max="contentFontSizeMax"
                :step="0.5"
                :input-number-props="sliderInputProps"
                @change="onProjectSummaryFontSizeChange"
              />
            </div>
          </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">简介行距</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.projectSummaryLineHeight"
                :min="lineHeightMin"
                :max="lineHeightMax"
                :step="lineHeightStep"
                :input-number-props="sliderInputProps"
                @update:model-value="resume.theme.projectSummaryLineHeight = clampLineHeight($event)"
              />
            </div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">亮点字号</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.projectHighlightsFontSize"
                :min="contentFontSizeMin"
                :max="contentFontSizeMax"
                :step="0.5"
                :input-number-props="sliderInputProps"
                @change="onProjectHighlightsFontSizeChange"
              />
            </div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">亮点行距</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.projectHighlightsLineHeight"
                :min="lineHeightMin"
                :max="lineHeightMax"
                :step="lineHeightStep"
                :input-number-props="sliderInputProps"
                @update:model-value="resume.theme.projectHighlightsLineHeight = clampLineHeight($event)"
              />
            </div>
          </div>
        </div>
        <div
          v-if="!resume.projects.length"
          class="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600"
        >
          暂无项目经历，点击下方按钮新增。
        </div>
        <draggable
          v-else
          v-model="resume.projects"
          item-key="id"
          handle=".drag-handle"
          class="drag-list"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
          :animation="180"
        >
          <template #item="{ element: item, index }">
            <article class="sub-card drag-item">
              <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <button type="button" class="drag-handle" title="拖拽排序">::</button>
                  <span class="text-sm font-semibold text-slate-800">项目 {{ index + 1 }}</span>
                  <span
                    v-if="item.hidden"
                    class="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700"
                  >
                    已隐藏（预览不显示）
                  </span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                  <button type="button" class="small-btn" @click="$emit('toggle-project-hidden', item.id)">
                    {{ item.hidden ? '显示' : '隐藏' }}
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === 0"
                    @click="$emit('move-project-up', index)"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === resume.projects.length - 1"
                    @click="$emit('move-project-down', index)"
                  >
                    下移
                  </button>
                  <button type="button" class="small-btn small-btn-danger" @click="$emit('remove-project', item.id)">
                    删除
                  </button>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <label class="field-wrap">
                  <span class="field-label">项目名称</span>
                  <input v-model="item.name" class="field-input" placeholder="例如：TsumiMusic" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">项目角色</span>
                  <input v-model="item.role" class="field-input" placeholder="例如：后端负责人" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">项目周期</span>
                  <input v-model="item.period" class="field-input" placeholder="例如：2025.09 - 2026.01" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">技术标签（逗号分隔）</span>
                  <input
                    v-model="item.tags"
                    class="field-input"
                    placeholder="例如：SpringBoot, Redis, MySQL"
                  />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">项目描述</span>
                  <textarea
                    v-model="item.summary"
                    v-auto-resize
                    class="field-input field-textarea"
                    placeholder="建议包含业务目标、核心能力和结果"
                  ></textarea>
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">项目亮点（每行一条）</span>
                  <textarea
                    v-model="item.highlights"
                    v-auto-resize
                    class="field-input field-textarea"
                    placeholder="例如：&#10;实现 JWT + RSA 非对称鉴权&#10;设计 Redis 缓存和限流策略"
                  ></textarea>
                </label>
              </div>
            </article>
          </template>
        </draggable>
        <button type="button" class="toolbar-btn w-full border-dashed" @click="$emit('add-project')">
          + 新增项目经历
        </button>
      </div>
    </article>

    <article class="panel-card" :style="{ order: getEditorSectionOrder('studentExperiences') }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'student')">
          <span class="panel-caret">{{ panels.student ? '▾' : '▸' }}</span>
          <span class="panel-title">学生经历</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.studentExperiences ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.studentExperiences = !resume.sectionVisibility.studentExperiences"
        >
          <browse-icon
            v-if="resume.sectionVisibility.studentExperiences"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
      <div v-if="panels.student" class="panel-body mt-4 space-y-3">
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">组织名称字号</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.studentNameFontSize"
                :min="emphasisFontSizeMin"
                :max="emphasisFontSizeMax"
                :step="0.5"
                :input-number-props="sliderInputProps"
                @change="onStudentNameFontSizeChange"
              />
            </div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">角色时间字号</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.studentMetaFontSize"
                :min="metaFontSizeMin"
                :max="metaFontSizeMax"
                :step="0.5"
                :input-number-props="sliderInputProps"
                @change="onStudentMetaFontSizeChange"
              />
            </div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">简介字号</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.studentSummaryFontSize"
                :min="contentFontSizeMin"
                :max="contentFontSizeMax"
                :step="0.5"
                :input-number-props="sliderInputProps"
                @change="onStudentSummaryFontSizeChange"
              />
            </div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <label class="field-label mb-2 block">亮点字号</label>
            <div class="setting-slider">
              <Slider
                :model-value="resume.theme.studentHighlightsFontSize"
                :min="contentFontSizeMin"
                :max="contentFontSizeMax"
                :step="0.5"
                :input-number-props="sliderInputProps"
                @change="onStudentHighlightsFontSizeChange"
              />
            </div>
          </div>
        </div>
        <div
          v-if="!resume.studentExperiences.length"
          class="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600"
        >
          暂无学生经历，点击下方按钮新增。
        </div>
        <draggable
          v-else
          v-model="resume.studentExperiences"
          item-key="id"
          handle=".drag-handle"
          class="drag-list"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
          :animation="180"
        >
          <template #item="{ element: item, index }">
            <article class="sub-card drag-item">
              <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <button type="button" class="drag-handle" title="拖拽排序">::</button>
                  <span class="text-sm font-semibold text-slate-800">学生 {{ index + 1 }}</span>
                  <span
                    v-if="item.hidden"
                    class="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700"
                  >
                    已隐藏（预览不显示）
                  </span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                  <button type="button" class="small-btn" @click="$emit('toggle-student-experience-hidden', item.id)">
                    {{ item.hidden ? '显示' : '隐藏' }}
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === 0"
                    @click="$emit('move-student-experience-up', index)"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === resume.studentExperiences.length - 1"
                    @click="$emit('move-student-experience-down', index)"
                  >
                    下移
                  </button>
                  <button type="button" class="small-btn small-btn-danger" @click="$emit('remove-student-experience', item.id)">
                    删除
                  </button>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <label class="field-wrap">
                  <span class="field-label">组织/单位</span>
                  <input v-model="item.organization" class="field-input" placeholder="例如：党委学生工作部教育科" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">角色/职务</span>
                  <input v-model="item.role" class="field-input" placeholder="例如：学生助理" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">时间</span>
                  <input v-model="item.period" class="field-input" placeholder="例如：2024.09 - 2025.06" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">简介</span>
                  <textarea v-model="item.summary" v-auto-resize class="field-input field-textarea" placeholder="简要描述职责与工作内容"></textarea>
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">成果亮点（每行一条）</span>
                  <textarea v-model="item.highlights" v-auto-resize class="field-input field-textarea" placeholder="每行一条，可用 **关键字** 强调"></textarea>
                </label>
              </div>
            </article>
          </template>
        </draggable>
        <button type="button" class="toolbar-btn w-full border-dashed" @click="$emit('add-student-experience')">
          + 新增学生经历
        </button>
      </div>
    </article>

    <article class="panel-card" :style="{ order: getEditorSectionOrder('customImages') }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'customImages')">
          <span class="panel-caret">{{ panels.customImages ? '▾' : '▸' }}</span>
          <span class="panel-title">图片展示</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.customImages ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.customImages = !resume.sectionVisibility.customImages"
        >
          <browse-icon
            v-if="resume.sectionVisibility.customImages"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
      <div v-if="panels.customImages" class="panel-body mt-4 space-y-3">
        <p class="rounded-lg border border-sky-100 bg-sky-50 px-3 py-2 text-sm leading-6 text-slate-600">
          可上传 GitHub 活跃图、作品截图、数据看板等图片，支持 JPG / PNG / WebP / SVG，单张不超过 5MB。
        </p>
        <div
          v-if="!resume.customImages.length"
          class="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600"
        >
          暂无图片展示，点击下方按钮新增。
        </div>
        <draggable
          v-else
          v-model="resume.customImages"
          item-key="id"
          handle=".drag-handle"
          class="drag-list"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
          :animation="180"
        >
          <template #item="{ element: item, index }">
            <article class="sub-card drag-item">
              <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <button type="button" class="drag-handle" title="拖拽排序">::</button>
                  <span class="text-sm font-semibold text-slate-800">图片 {{ index + 1 }}</span>
                  <span
                    v-if="item.hidden"
                    class="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700"
                  >
                    已隐藏（预览不显示）
                  </span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                  <button type="button" class="small-btn" @click="$emit('toggle-custom-image-hidden', item.id)">
                    {{ item.hidden ? '显示' : '隐藏' }}
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === 0"
                    @click="$emit('move-custom-image-up', index)"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === resume.customImages.length - 1"
                    @click="$emit('move-custom-image-down', index)"
                  >
                    下移
                  </button>
                  <button type="button" class="small-btn small-btn-danger" @click="$emit('remove-custom-image', item.id)">
                    删除
                  </button>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <label class="field-wrap">
                  <span class="field-label">标题</span>
                  <input v-model="item.title" class="field-input" placeholder="例如：GitHub 活跃度" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">英文副标题</span>
                  <input v-model="item.subtitle" class="field-input" placeholder="例如：GITHUB ACTIVITY" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">替代文本</span>
                  <input v-model="item.alt" class="field-input" placeholder="用于导出和无障碍说明，可留空" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">说明文字</span>
                  <input v-model="item.caption" class="field-input" placeholder="可写图片来源或成果说明，不填写则不展示" />
                </label>
                <div class="field-wrap sm:col-span-2">
                  <span class="field-label">图片文件</span>
                  <div class="mt-2 flex flex-wrap items-center gap-3">
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp,image/svg+xml"
                      class="file-input"
                      @change="$emit('custom-image-change', item.id, $event)"
                    />
                    <button
                      type="button"
                      class="toolbar-btn !px-3 !py-1.5 !text-xs"
                      @click="$emit('remove-custom-image-file', item.id)"
                    >
                      移除图片
                    </button>
                  </div>
                </div>
                <div class="field-wrap sm:col-span-2">
                  <span class="field-label">展示宽度（40%-100%）</span>
                  <div class="mt-2 flex items-center gap-2">
                    <input v-model.number="item.widthPercent" type="range" min="40" max="100" class="w-full accent-sky-600" />
                    <input
                      v-model.number="item.widthPercent"
                      type="number"
                      min="40"
                      max="100"
                      class="field-input !w-24 !px-2 !py-1.5 text-center"
                    />
                  </div>
                </div>
              </div>
            </article>
          </template>
        </draggable>
        <button type="button" class="toolbar-btn w-full border-dashed" @click="$emit('add-custom-image')">
          + 新增图片展示
        </button>
      </div>
    </article>

    <article class="panel-card" :style="{ order: getEditorSectionOrder('awards') }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'awards')">
          <span class="panel-caret">{{ panels.awards ? '▾' : '▸' }}</span>
          <span class="panel-title">荣誉奖项</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.awards ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.awards = !resume.sectionVisibility.awards"
        >
          <browse-icon
            v-if="resume.sectionVisibility.awards"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
        <div v-if="panels.awards" class="panel-body mt-4 space-y-3">
          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">标题字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="resume.theme.awardTitleFontSize"
                  :min="emphasisFontSizeMin"
                  :max="emphasisFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @change="onAwardTitleFontSizeChange"
                />
              </div>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">元信息字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="resume.theme.awardMetaFontSize"
                  :min="metaFontSizeMin"
                  :max="metaFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @change="onAwardMetaFontSizeChange"
                />
              </div>
            </div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">描述字号</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.awardDescriptionFontSize"
              :min="contentFontSizeMin"
              :max="contentFontSizeMax"
              :step="0.5"
              :input-number-props="sliderInputProps"
              @change="onAwardDescriptionFontSizeChange"
            />
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">描述行距</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.awardDescriptionLineHeight"
              :min="lineHeightMin"
              :max="lineHeightMax"
              :step="lineHeightStep"
              :input-number-props="sliderInputProps"
              @update:model-value="resume.theme.awardDescriptionLineHeight = clampLineHeight($event)"
            />
          </div>
        </div>
        <div
          v-if="!resume.awards.length"
          class="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600"
        >
          暂无荣誉奖项，点击下方按钮新增。
        </div>
        <draggable
          v-else
          v-model="resume.awards"
          item-key="id"
          handle=".drag-handle"
          class="drag-list"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
          :animation="180"
        >
          <template #item="{ element: item, index }">
            <article class="sub-card drag-item">
              <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <button type="button" class="drag-handle" title="拖拽排序">::</button>
                  <span class="text-sm font-semibold text-slate-800">奖项 {{ index + 1 }}</span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                  <button type="button" class="small-btn" @click="$emit('toggle-award-hidden', item.id)">
                    {{ item.hidden ? '显示' : '隐藏' }}
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === 0"
                    @click="$emit('move-award-up', index)"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === resume.awards.length - 1"
                    @click="$emit('move-award-down', index)"
                  >
                    下移
                  </button>
                  <button type="button" class="small-btn small-btn-danger" @click="$emit('remove-award', item.id)">
                    删除
                  </button>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <label class="field-wrap">
                  <span class="field-label">奖项名称</span>
                  <input v-model="item.name" class="field-input" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">获奖级别</span>
                  <input v-model="item.level" class="field-input" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">颁发单位</span>
                  <input v-model="item.issuer" class="field-input" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">获奖时间</span>
                  <input v-model="item.date" class="field-input" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">补充描述</span>
                  <textarea v-model="item.description" v-auto-resize class="field-input field-textarea"></textarea>
                </label>
              </div>
            </article>
          </template>
        </draggable>
        <button type="button" class="toolbar-btn w-full border-dashed" @click="$emit('add-award')">
          + 新增荣誉奖项
        </button>
      </div>
    </article>

    <article class="panel-card" :style="{ order: getEditorSectionOrder('certificates') }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'certificates')">
          <span class="panel-caret">{{ panels.certificates ? '▾' : '▸' }}</span>
          <span class="panel-title">证书</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.certificates ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.certificates = !resume.sectionVisibility.certificates"
        >
          <browse-icon
            v-if="resume.sectionVisibility.certificates"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
        <div v-if="panels.certificates" class="panel-body mt-4 space-y-3">
          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">标题字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="resume.theme.certificateTitleFontSize"
                  :min="emphasisFontSizeMin"
                  :max="emphasisFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @change="onCertificateTitleFontSizeChange"
                />
              </div>
            </div>
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <label class="field-label mb-2 block">元信息字号</label>
              <div class="setting-slider">
                <Slider
                  :model-value="resume.theme.certificateMetaFontSize"
                  :min="metaFontSizeMin"
                  :max="metaFontSizeMax"
                  :step="0.5"
                  :input-number-props="sliderInputProps"
                  @change="onCertificateMetaFontSizeChange"
                />
              </div>
            </div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">描述字号</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.certificateDescriptionFontSize"
              :min="contentFontSizeMin"
              :max="contentFontSizeMax"
              :step="0.5"
              :input-number-props="sliderInputProps"
              @change="onCertificateDescriptionFontSizeChange"
            />
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">描述行距</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.certificateDescriptionLineHeight"
              :min="lineHeightMin"
              :max="lineHeightMax"
              :step="lineHeightStep"
              :input-number-props="sliderInputProps"
              @update:model-value="resume.theme.certificateDescriptionLineHeight = clampLineHeight($event)"
            />
          </div>
        </div>
        <div
          v-if="!resume.certificates.length"
          class="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600"
        >
          暂无证书，点击下方按钮新增。
        </div>
        <draggable
          v-else
          v-model="resume.certificates"
          item-key="id"
          handle=".drag-handle"
          class="drag-list"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
          :animation="180"
        >
          <template #item="{ element: item, index }">
            <article class="sub-card drag-item">
              <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <button type="button" class="drag-handle" title="拖拽排序">::</button>
                  <span class="text-sm font-semibold text-slate-800">证书 {{ index + 1 }}</span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                  <button type="button" class="small-btn" @click="$emit('toggle-certificate-hidden', item.id)">
                    {{ item.hidden ? '显示' : '隐藏' }}
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === 0"
                    @click="$emit('move-certificate-up', index)"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    class="small-btn disabled:cursor-not-allowed disabled:opacity-40"
                    :disabled="index === resume.certificates.length - 1"
                    @click="$emit('move-certificate-down', index)"
                  >
                    下移
                  </button>
                  <button
                    type="button"
                    class="small-btn small-btn-danger"
                    @click="$emit('remove-certificate', item.id)"
                  >
                    删除
                  </button>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <label class="field-wrap">
                  <span class="field-label">证书名称</span>
                  <input v-model="item.name" class="field-input" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">颁发机构</span>
                  <input v-model="item.issuer" class="field-input" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">获得时间</span>
                  <input v-model="item.date" class="field-input" />
                </label>
                <label class="field-wrap">
                  <span class="field-label">证书编号</span>
                  <input v-model="item.credentialId" class="field-input" />
                </label>
                <label class="field-wrap sm:col-span-2">
                  <span class="field-label">补充描述</span>
                  <textarea v-model="item.description" v-auto-resize class="field-input field-textarea"></textarea>
                </label>
              </div>
            </article>
          </template>
        </draggable>
        <button type="button" class="toolbar-btn w-full border-dashed" @click="$emit('add-certificate')">
          + 新增证书
        </button>
      </div>
    </article>

    <article class="panel-card" :style="{ order: getEditorSectionOrder('selfSummary') }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'selfSummary')">
          <span class="panel-caret">{{ panels.selfSummary ? '▾' : '▸' }}</span>
          <span class="panel-title">自我评价</span>
        </button>
        <button
          type="button"
          class="panel-eye"
          :class="resume.sectionVisibility.selfSummary ? '' : 'panel-eye-off'"
          @click="resume.sectionVisibility.selfSummary = !resume.sectionVisibility.selfSummary"
        >
          <browse-icon
            v-if="resume.sectionVisibility.selfSummary"
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
          <browse-off-icon
            v-else
            :fill-color="['transparent', 'transparent']"
            :stroke-color="['currentColor', '#0052d9']"
            :stroke-width="2"
          />
        </button>
      </div>
      <div v-if="panels.selfSummary" class="panel-body mt-4 space-y-3">
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">正文字号</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.selfSummaryFontSize"
              :min="contentFontSizeMin"
              :max="contentFontSizeMax"
              :step="0.5"
              :input-number-props="sliderInputProps"
              @change="onSelfSummaryFontSizeChange"
            />
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">正文行距</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.selfSummaryLineHeight"
              :min="lineHeightMin"
              :max="lineHeightMax"
              :step="lineHeightStep"
              :input-number-props="sliderInputProps"
              @update:model-value="resume.theme.selfSummaryLineHeight = clampLineHeight($event)"
            />
          </div>
        </div>
        <label class="field-wrap">
          <span class="field-label">内容</span>
          <textarea v-model="resume.selfSummary.content" v-auto-resize class="field-input field-textarea"></textarea>
        </label>
        <label class="switch-field">
          <input v-model="resume.selfSummary.hidden" type="checkbox" class="h-4 w-4 accent-sky-600" />
          <span>仅隐藏这段内容（预览不显示）</span>
        </label>
      </div>
    </article>

    <article class="panel-card" :style="{ order: 99 }">
      <div class="panel-head">
        <button type="button" class="panel-head-main" @click="$emit('toggle-panel', 'theme')">
          <span class="panel-caret">{{ panels.theme ? '▾' : '▸' }}</span>
          <span class="panel-title">主题配置</span>
        </button>
      </div>
      <div v-if="panels.theme" class="panel-body mt-4 space-y-4">
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">姓名字体</label>
          <select v-model="resume.theme.nameFont" class="field-input">
            <option v-for="option in nameFontOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">姓名字号</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.nameFontSize"
              :min="nameFontSizeMin"
              :max="nameFontSizeMax"
              :step="0.5"
              :input-number-props="sliderInputProps"
              @change="onNameFontSizeChange"
            />
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">学校字体</label>
          <select v-model="resume.theme.schoolFont" class="field-input">
            <option v-for="option in nameFontOptions" :key="`school-${option.value}`" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">学校字号</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.schoolFontSize"
              :min="schoolFontSizeMin"
              :max="schoolFontSizeMax"
              :step="0.5"
              :input-number-props="sliderInputProps"
              @change="onSchoolFontSizeChange"
            />
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">模块间距</label>
          <div class="setting-slider">
            <Slider
              :model-value="resume.theme.sectionGap"
              :min="sectionGapMin"
              :max="sectionGapMax"
              :step="1"
              :input-number-props="sliderInputProps"
              @update:model-value="resume.theme.sectionGap = clampSectionGap($event)"
            />
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">主题色（支持透明）</label>
          <div class="color-panel-wrap">
            <ColorPicker
              :model-value="getColorValue(resume.theme.primaryColor)"
              format="RGBA"
              :enable-alpha="true"
              :show-primary-color-preview="false"
              :color-modes="['monochrome', 'linear-gradient']"
              @update:model-value="resume.theme.primaryColor = $event"
              @change="resume.theme.primaryColor = $event"
            />
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">姓名颜色</label>
          <div class="flex items-center gap-2">
            <div class="color-panel-wrap">
              <ColorPicker
                :model-value="getColorValue(resume.theme.nameColor, resume.theme.primaryColor || '#4a9fff')"
                format="RGBA"
                :enable-alpha="true"
                :show-primary-color-preview="false"
                :color-modes="['monochrome', 'linear-gradient']"
                @update:model-value="resume.theme.nameColor = $event"
                @change="resume.theme.nameColor = $event"
              />
            </div>
            <button type="button" class="small-btn" @click="resume.theme.nameColor = ''">恢复默认</button>
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
          <label class="field-label mb-2 block">学校颜色</label>
          <div class="flex items-center gap-2">
            <div class="color-panel-wrap">
              <ColorPicker
                :model-value="getColorValue(resume.theme.schoolColor, resume.theme.primaryColor || '#4a9fff')"
                format="RGBA"
                :enable-alpha="true"
                :show-primary-color-preview="false"
                :color-modes="['monochrome', 'linear-gradient']"
                @update:model-value="resume.theme.schoolColor = $event"
                @change="resume.theme.schoolColor = $event"
              />
            </div>
            <button type="button" class="small-btn" @click="resume.theme.schoolColor = ''">恢复默认</button>
          </div>
        </div>
        <label class="switch-field">
          <input v-model="resume.theme.boldMajor" type="checkbox" class="h-4 w-4 accent-sky-600" />
          <span>专业加粗显示</span>
        </label>
      </div>
    </article>
  </aside>
</template>
