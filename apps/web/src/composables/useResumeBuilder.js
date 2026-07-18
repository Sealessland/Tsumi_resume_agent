import { computed, reactive, ref, watch, watchEffect } from 'vue'
import { PANELS_STORAGE_KEY, SCHEMA_VERSION } from '../modules/resume/constants'
import {
  createAwardItem,
  createCertificateItem,
  createCustomImageItem,
  createEducationItem,
  createInternshipItem,
  createPanelsState,
  createProjectItem,
  createResearchExperienceItem,
  createStudentExperienceItem,
} from '../modules/resume/factories'
import { normalizeResumeData } from '../modules/resume/normalize'
import { normalizeLayoutOrder } from '../modules/resume/sections'
import { createDemoResume, createEmptyResume } from '../modules/resume/templates'
import { processProfilePhoto } from '../modules/resume/photo'
import { deepBrandFrom, nameBrandFrom, schoolBrandFrom } from '../modules/resume/color'
import { getNameFontFamily, getSchoolFontFamily } from '../modules/resume/nameFont'
import {
  clampLineHeight,
  clampSectionGap,
  clampEducationLogoSize,
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
  clampNameFontSize,
  clampProjectMetaFontSize,
  clampProjectHighlightsFontSize,
  clampProjectNameFontSize,
  clampProjectSummaryFontSize,
  clampProjectTagFontSize,
  clampStudentHighlightsFontSize,
  clampStudentMetaFontSize,
  clampStudentNameFontSize,
  clampStudentSummaryFontSize,
  clampSchoolFontSize,
  clampSelfSummaryFontSize,
  clampSkillsFontSize,
} from '../modules/resume/typography'
import {
  deleteResumeDraft,
  loadResumeDraft,
  migrateLegacyDraftIfNeeded,
  saveResumeDraft,
} from '../modules/resume/storage'
import {
  createResumeAsset,
  deleteResumeAsset,
  listResumeAssets,
  loadActiveResumeAssetId,
  normalizeResumeAsset,
  saveActiveResumeAssetId,
  saveResumeAsset,
} from '../modules/resume/assets'

const EDUCATION_LOGO_MAX_BYTES = 2 * 1024 * 1024
const EDUCATION_LOGO_SUPPORTED_TYPES = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp'])
const CUSTOM_IMAGE_MAX_BYTES = 5 * 1024 * 1024
const CUSTOM_IMAGE_SUPPORTED_TYPES = new Set([
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/webp',
  'image/svg+xml',
])
const AUTO_SAVE_DELAY = 800

function moveItem(list, fromIndex, toIndex) {
  if (toIndex < 0 || toIndex >= list.length || fromIndex < 0 || fromIndex >= list.length) return
  const [target] = list.splice(fromIndex, 1)
  list.splice(toIndex, 0, target)
}

export function useResumeBuilder() {
  const resume = reactive(normalizeResumeData(createDemoResume()))
  const panels = reactive(createPanelsState())
  const resumeAssets = ref([])
  const currentAssetId = ref('')
  const assetsLoading = ref(true)
  const photoUploadMessage = ref('')
  const photoUploadError = ref('')
  const jsonStatusMessage = ref('')
  const jsonErrorMessage = ref('')
  const actionStatusMessage = ref('')
  const actionErrorMessage = ref('')
  const exportWarningMessage = ref('')
  const educationLogoFeedback = reactive({ id: '', message: '', error: '' })
  const jsonInputRef = ref(null)
  const pageOverflow = ref(false)
  const pageHeight = ref(0)
  const storageBackend = ref('indexeddb')

  let autoSaveTimer = null
  let bootstrapping = true
  let skipNextAutoSave = false
  let hasShownFallbackNotice = false

  function restorePanelsState() {
    const raw = localStorage.getItem(PANELS_STORAGE_KEY)
    if (!raw) return
    try {
      const parsed = JSON.parse(raw)
      if (!parsed || typeof parsed !== 'object') return
      Object.keys(panels).forEach((key) => {
        if (Object.prototype.hasOwnProperty.call(parsed, key)) panels[key] = Boolean(parsed[key])
      })
    } catch (error) {
      console.error('Restore panels failed:', error)
    }
  }

  function savePanelsState() {
    localStorage.setItem(PANELS_STORAGE_KEY, JSON.stringify(panels))
  }

  function applyResumeData(source) {
    Object.assign(resume, normalizeResumeData(source))
  }

  function createResumeSnapshot() {
    return normalizeResumeData(JSON.parse(JSON.stringify(resume)))
  }

  const currentResumeAsset = computed(() =>
    resumeAssets.value.find((asset) => asset.id === currentAssetId.value) || null
  )

  function replaceAsset(asset) {
    const index = resumeAssets.value.findIndex((item) => item.id === asset.id)
    if (index === -1) resumeAssets.value.unshift(asset)
    else resumeAssets.value.splice(index, 1, asset)
  }

  async function persistCurrentAsset() {
    const current = currentResumeAsset.value
    if (!current) return
    replaceAsset(await saveResumeAsset({ ...current, resume: createResumeSnapshot() }))
  }

  async function selectResumeAsset(id) {
    const target = resumeAssets.value.find((asset) => asset.id === id)
    if (!target || target.id === currentAssetId.value) return
    await persistCurrentAsset()
    currentAssetId.value = target.id
    await saveActiveResumeAssetId(target.id)
    skipNextAutoSave = true
    applyResumeData(target.resume)
    actionStatusMessage.value = `已切换到「${target.title || target.targetRole || '未命名简历'}」，AI 将处理此版本。`
  }

  async function createResumeAssetVersion(type = 'blank') {
    await persistCurrentAsset()
    const asset = await saveResumeAsset(createResumeAsset({
      title: type === 'demo' ? '示例岗位简历' : '新岗位简历',
      resume: normalizeResumeData(type === 'demo' ? createDemoResume() : createEmptyResume()),
    }))
    replaceAsset(asset)
    currentAssetId.value = asset.id
    await saveActiveResumeAssetId(asset.id)
    skipNextAutoSave = true
    applyResumeData(asset.resume)
  }

  async function duplicateResumeAsset(id) {
    const source = resumeAssets.value.find((asset) => asset.id === id)
    if (!source) return
    if (source.id === currentAssetId.value) await persistCurrentAsset()
    const duplicate = await saveResumeAsset(createResumeAsset({
      ...source,
      id: '',
      createdAt: '',
      title: `${source.title || '岗位简历'} · 新岗位版`,
      targetCompany: '',
      targetRole: '',
      jobDescription: '',
      status: 'draft',
      resume: JSON.parse(JSON.stringify(source.resume)),
    }))
    replaceAsset(duplicate)
  }

  async function updateResumeAssetMetadata(id, patch) {
    const source = resumeAssets.value.find((asset) => asset.id === id)
    if (!source) return
    replaceAsset(await saveResumeAsset({ ...source, ...patch }))
  }

  async function archiveResumeAsset(id) {
    await updateResumeAssetMetadata(id, { status: 'archived' })
  }

  async function restoreResumeAsset(id) {
    await updateResumeAssetMetadata(id, { status: 'draft' })
  }

  async function removeResumeAsset(id) {
    if (resumeAssets.value.length <= 1) {
      actionErrorMessage.value = '至少保留一份岗位简历。'
      return
    }
    await deleteResumeAsset(id)
    resumeAssets.value = resumeAssets.value.filter((asset) => asset.id !== id)
    if (currentAssetId.value === id) {
      currentAssetId.value = resumeAssets.value[0].id
      await saveActiveResumeAssetId(currentAssetId.value)
      applyResumeData(resumeAssets.value[0].resume)
    }
  }

  function resetPhotoFeedback() {
    photoUploadMessage.value = ''
    photoUploadError.value = ''
  }

  function resetEducationLogoFeedback(id = '') {
    educationLogoFeedback.id = id
    educationLogoFeedback.message = ''
    educationLogoFeedback.error = ''
  }

  function clearActionFeedback() {
    actionStatusMessage.value = ''
    actionErrorMessage.value = ''
  }

  function scheduleAutoSave() {
    if (bootstrapping) return
    if (skipNextAutoSave) {
      skipNextAutoSave = false
      return
    }
    if (autoSaveTimer) window.clearTimeout(autoSaveTimer)
    autoSaveTimer = window.setTimeout(() => persistDraft({ manual: false }), AUTO_SAVE_DELAY)
  }

  async function persistDraft({ manual = false } = {}) {
    if (autoSaveTimer) {
      window.clearTimeout(autoSaveTimer)
      autoSaveTimer = null
    }
    if (manual) clearActionFeedback()
    try {
      const result = await saveResumeDraft(createResumeSnapshot())
      storageBackend.value = result.backend
      await persistCurrentAsset()
      if (result.backend === 'localstorage') {
        if (!hasShownFallbackNotice) {
          actionErrorMessage.value = 'IndexedDB 不可用，已回退到浏览器本地轻量存储。'
          hasShownFallbackNotice = true
        }
        if (manual) actionStatusMessage.value = '岗位简历已保存到浏览器本地存储。'
        return
      }
      hasShownFallbackNotice = false
      if (manual) actionStatusMessage.value = '岗位简历已保存到浏览器本地数据库。'
    } catch (error) {
      console.error('Save draft failed:', error)
      actionErrorMessage.value = '岗位简历保存失败，请稍后重试。'
    }
  }

  function togglePanel(name) {
    panels[name] = !panels[name]
    savePanelsState()
  }

  function expandAllPanels() {
    Object.keys(panels).forEach((key) => { panels[key] = true })
    savePanelsState()
  }

  function collapseAllPanels() {
    Object.keys(panels).forEach((key) => { panels[key] = false })
    savePanelsState()
  }

  function loadDemo() {
    clearActionFeedback()
    applyResumeData(createDemoResume())
    resetPhotoFeedback()
    resetEducationLogoFeedback()
  }

  async function clearAll() {
    clearActionFeedback()
    skipNextAutoSave = true
    applyResumeData(createEmptyResume())
    resetPhotoFeedback()
    resetEducationLogoFeedback()
    try {
      await deleteResumeDraft()
      await persistCurrentAsset()
      actionStatusMessage.value = '已清空当前岗位简历内容。'
    } catch (error) {
      console.error('Clear draft failed:', error)
      actionErrorMessage.value = '内容已清空，但本地保存失败。'
    }
  }

  async function saveDraft() {
    await persistDraft({ manual: true })
  }

  async function restoreDraft() {
    clearActionFeedback()
    try {
      const draft = await loadResumeDraft()
      if (!draft) {
        actionErrorMessage.value = '当前没有可恢复的本地草稿。'
        return
      }
      skipNextAutoSave = true
      applyResumeData(draft)
      await persistCurrentAsset()
      resetPhotoFeedback()
      resetEducationLogoFeedback()
      actionStatusMessage.value = '本地草稿已恢复。'
    } catch (error) {
      console.error('Restore draft failed:', error)
      actionErrorMessage.value = '恢复本地草稿失败，请稍后重试。'
    }
  }

  function hasVisibleSection() {
    return Object.values(resume.sectionVisibility).some(Boolean)
  }

  function hasCoreContent() {
    return Boolean(
      resume.skills.trim()
      || resume.internships.some((item) => !item.hidden && (item.company || item.summary || item.highlights))
      || resume.researchExperiences.some((item) => !item.hidden && (item.title || item.summary || item.highlights))
      || resume.projects.some((item) => !item.hidden && (item.name || item.summary || item.highlights))
      || resume.studentExperiences.some((item) => !item.hidden && (item.organization || item.summary || item.highlights))
      || resume.customImages.some((item) => !item.hidden && item.image)
    )
  }

  function validateBeforeExport() {
    const warnings = []
    if (!String(resume.profile.name || '').trim()) warnings.push('姓名未填写')
    if (!hasVisibleSection()) warnings.push('所有栏目当前都处于隐藏状态')
    if (!hasCoreContent()) warnings.push('技术栈、经历或展示内容目前都为空')
    exportWarningMessage.value = warnings.length ? `导出提醒：${warnings.join('；')}。` : ''
  }

  function exportPdf() {
    validateBeforeExport()
    window.print()
  }

  function exportJson() {
    const content = JSON.stringify(createResumeSnapshot(), null, 2)
    const blob = new Blob([content], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `${resume.profile.name || 'resume'}.json`
    anchor.click()
    URL.revokeObjectURL(url)
    jsonStatusMessage.value = '当前岗位简历已导出为 JSON。'
  }

  function triggerJsonImport() {
    jsonInputRef.value?.click()
  }

  function handleJsonImport(event) {
    const input = event.target
    const file = input.files?.[0]
    input.value = ''
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => {
      try {
        applyResumeData(JSON.parse(String(reader.result || '')))
        jsonStatusMessage.value = 'JSON 简历已导入到当前岗位版本。'
      } catch (error) {
        jsonErrorMessage.value = 'JSON 文件格式无效，无法导入。'
      }
    }
    reader.onerror = () => { jsonErrorMessage.value = 'JSON 文件读取失败，请重试。' }
    reader.readAsText(file)
  }

  function addEducation() { resume.educations.push(createEducationItem()) }
  function removeEducation(id) { resume.educations = resume.educations.filter((item) => item.id !== id) }
  function toggleEducationHidden(id) { const item = resume.educations.find((entry) => entry.id === id); if (item) item.hidden = !item.hidden }
  function moveEducationUp(index) { moveItem(resume.educations, index, index - 1) }
  function moveEducationDown(index) { moveItem(resume.educations, index, index + 1) }
  function addInternship() { resume.internships.push(createInternshipItem()) }
  function removeInternship(id) { resume.internships = resume.internships.filter((item) => item.id !== id) }
  function toggleInternshipHidden(id) { const item = resume.internships.find((entry) => entry.id === id); if (item) item.hidden = !item.hidden }
  function moveInternshipUp(index) { moveItem(resume.internships, index, index - 1) }
  function moveInternshipDown(index) { moveItem(resume.internships, index, index + 1) }
  function addResearchExperience() { resume.researchExperiences.push(createResearchExperienceItem()) }
  function removeResearchExperience(id) { resume.researchExperiences = resume.researchExperiences.filter((item) => item.id !== id) }
  function toggleResearchExperienceHidden(id) { const item = resume.researchExperiences.find((entry) => entry.id === id); if (item) item.hidden = !item.hidden }
  function moveResearchExperienceUp(index) { moveItem(resume.researchExperiences, index, index - 1) }
  function moveResearchExperienceDown(index) { moveItem(resume.researchExperiences, index, index + 1) }
  function addProject() { resume.projects.push(createProjectItem()) }
  function removeProject(id) { resume.projects = resume.projects.filter((item) => item.id !== id) }
  function toggleProjectHidden(id) { const item = resume.projects.find((entry) => entry.id === id); if (item) item.hidden = !item.hidden }
  function moveProjectUp(index) { moveItem(resume.projects, index, index - 1) }
  function moveProjectDown(index) { moveItem(resume.projects, index, index + 1) }
  function addStudentExperience() { resume.studentExperiences.push(createStudentExperienceItem()) }
  function removeStudentExperience(id) { resume.studentExperiences = resume.studentExperiences.filter((item) => item.id !== id) }
  function toggleStudentExperienceHidden(id) { const item = resume.studentExperiences.find((entry) => entry.id === id); if (item) item.hidden = !item.hidden }
  function moveStudentExperienceUp(index) { moveItem(resume.studentExperiences, index, index - 1) }
  function moveStudentExperienceDown(index) { moveItem(resume.studentExperiences, index, index + 1) }
  function addCustomImage() { resume.customImages.push(createCustomImageItem()) }
  function removeCustomImage(id) { resume.customImages = resume.customImages.filter((item) => item.id !== id) }
  function toggleCustomImageHidden(id) { const item = resume.customImages.find((entry) => entry.id === id); if (item) item.hidden = !item.hidden }
  function moveCustomImageUp(index) { moveItem(resume.customImages, index, index - 1) }
  function moveCustomImageDown(index) { moveItem(resume.customImages, index, index + 1) }
  function addAward() { resume.awards.push(createAwardItem()) }
  function removeAward(id) { resume.awards = resume.awards.filter((item) => item.id !== id) }
  function toggleAwardHidden(id) { const item = resume.awards.find((entry) => entry.id === id); if (item) item.hidden = !item.hidden }
  function moveAwardUp(index) { moveItem(resume.awards, index, index - 1) }
  function moveAwardDown(index) { moveItem(resume.awards, index, index + 1) }
  function addCertificate() { resume.certificates.push(createCertificateItem()) }
  function removeCertificate(id) { resume.certificates = resume.certificates.filter((item) => item.id !== id) }
  function toggleCertificateHidden(id) { const item = resume.certificates.find((entry) => entry.id === id); if (item) item.hidden = !item.hidden }
  function moveCertificateUp(index) { moveItem(resume.certificates, index, index - 1) }
  function moveCertificateDown(index) { moveItem(resume.certificates, index, index + 1) }

  function onPhotoChange(event) {
    const input = event.target
    const file = input.files?.[0]
    input.value = ''
    if (!file) return
    processProfilePhoto(file).then((result) => {
      resume.profile.photo = result.dataUrl
      resume.profile.photoMeta = result.meta
      photoUploadMessage.value = result.message
      photoUploadError.value = ''
    }).catch((error) => {
      console.error(error)
      photoUploadError.value = '图片处理失败，请重试。'
    })
  }

  function removePhoto() { resume.profile.photo = ''; resume.profile.photoMeta = null }
  function onEducationLogoChange(event, id) { const file = event.target.files?.[0]; event.target.value = ''; if (!file) return; if (!EDUCATION_LOGO_SUPPORTED_TYPES.has(file.type) || file.size > EDUCATION_LOGO_MAX_BYTES) { educationLogoFeedback.error = '请上传 2MB 内的 JPG、PNG 或 WebP 图片。'; return }; const reader = new FileReader(); reader.onload = () => { const item = resume.educations.find((entry) => entry.id === id); if (item) item.logo = String(reader.result || '') }; reader.readAsDataURL(file) }
  function removeEducationLogo(id) { const item = resume.educations.find((entry) => entry.id === id); if (item) item.logo = '' }
  function onLogoChange(event, id) { const file = event.target.files?.[0]; event.target.value = ''; if (!file) return; const reader = new FileReader(); reader.onload = () => { const item = resume.internships.find((entry) => entry.id === id); if (item) item.logo = String(reader.result || '') }; reader.readAsDataURL(file) }
  function removeLogo(id) { const item = resume.internships.find((entry) => entry.id === id); if (item) item.logo = '' }
  function onCustomImageChange(event, id) { const file = event.target.files?.[0]; event.target.value = ''; if (!file) return; if (!CUSTOM_IMAGE_SUPPORTED_TYPES.has(file.type) || file.size > CUSTOM_IMAGE_MAX_BYTES) return; const reader = new FileReader(); reader.onload = () => { const item = resume.customImages.find((entry) => entry.id === id); if (item) item.image = String(reader.result || '') }; reader.readAsDataURL(file) }
  function removeCustomImageFile(id) { const item = resume.customImages.find((entry) => entry.id === id); if (item) item.image = '' }
  function onPageOverflowChange(payload) { pageOverflow.value = Boolean(payload?.overflow); pageHeight.value = Math.round(Number(payload?.height) || 0) }
  function updateLayoutOrder(order) { resume.layout.order = normalizeLayoutOrder(order) }

  async function bootstrapResumeAssets() {
    assetsLoading.value = true
    try {
      const assets = await listResumeAssets()
      if (!assets.length) {
        const draft = await loadResumeDraft()
        const initial = await saveResumeAsset(createResumeAsset({ title: '我的岗位简历', status: 'active', resume: normalizeResumeData(draft || resume) }))
        resumeAssets.value = [initial]
        currentAssetId.value = initial.id
        await saveActiveResumeAssetId(initial.id)
        skipNextAutoSave = true
        applyResumeData(initial.resume)
      } else {
        resumeAssets.value = assets
        const activeId = await loadActiveResumeAssetId()
        const active = assets.find((asset) => asset.id === activeId) || assets[0]
        currentAssetId.value = active.id
        await saveActiveResumeAssetId(active.id)
        skipNextAutoSave = true
        applyResumeData(active.resume)
      }
    } catch (error) {
      console.error('Bootstrap resume assets failed:', error)
      actionErrorMessage.value = '岗位简历资产库初始化失败，当前仍可继续编辑。'
    } finally {
      assetsLoading.value = false
      bootstrapping = false
    }
  }

  watch(resume, () => scheduleAutoSave(), { deep: true })

  const brandStyle = computed(() => ({
    '--brand': resume.theme.primaryColor || '#4a9fff',
    '--brand-deep': deepBrandFrom(resume.theme.primaryColor),
    '--brand-name': resume.theme.nameColor || nameBrandFrom(resume.theme.primaryColor),
    '--brand-school': resume.theme.schoolColor || schoolBrandFrom(resume.theme.primaryColor),
    '--name-font': getNameFontFamily(resume.theme.nameFont),
    '--name-font-size': `${clampNameFontSize(resume.theme.nameFontSize)}px`,
    '--school-font': getSchoolFontFamily(resume.theme.schoolFont),
    '--school-font-size': `${clampSchoolFontSize(resume.theme.schoolFontSize)}px`,
    '--skills-font-size': `${clampSkillsFontSize(resume.theme.skillsFontSize)}px`,
    '--internship-summary-font-size': `${clampInternshipSummaryFontSize(resume.theme.internshipSummaryFontSize)}px`,
    '--internship-highlights-font-size': `${clampInternshipHighlightsFontSize(resume.theme.internshipHighlightsFontSize)}px`,
    '--internship-time-font-size': `${clampInternshipTimeFontSize(resume.theme.internshipTimeFontSize)}px`,
    '--internship-company-meta-font-size': `${clampInternshipCompanyMetaFontSize(resume.theme.internshipCompanyMetaFontSize)}px`,
    '--internship-role-font-size': `${clampInternshipRoleFontSize(resume.theme.internshipRoleFontSize)}px`,
    '--project-summary-font-size': `${clampProjectSummaryFontSize(resume.theme.projectSummaryFontSize)}px`,
    '--project-highlights-font-size': `${clampProjectHighlightsFontSize(resume.theme.projectHighlightsFontSize)}px`,
    '--project-name-font-size': `${clampProjectNameFontSize(resume.theme.projectNameFontSize)}px`,
    '--project-meta-font-size': `${clampProjectMetaFontSize(resume.theme.projectMetaFontSize)}px`,
    '--project-tag-font-size': `${clampProjectTagFontSize(resume.theme.projectTagFontSize)}px`,
    '--student-name-font-size': `${clampStudentNameFontSize(resume.theme.studentNameFontSize)}px`,
    '--student-meta-font-size': `${clampStudentMetaFontSize(resume.theme.studentMetaFontSize)}px`,
    '--student-summary-font-size': `${clampStudentSummaryFontSize(resume.theme.studentSummaryFontSize)}px`,
    '--student-highlights-font-size': `${clampStudentHighlightsFontSize(resume.theme.studentHighlightsFontSize)}px`,
    '--award-title-font-size': `${clampAwardTitleFontSize(resume.theme.awardTitleFontSize)}px`,
    '--award-meta-font-size': `${clampAwardMetaFontSize(resume.theme.awardMetaFontSize)}px`,
    '--award-description-font-size': `${clampAwardDescriptionFontSize(resume.theme.awardDescriptionFontSize)}px`,
    '--certificate-title-font-size': `${clampCertificateTitleFontSize(resume.theme.certificateTitleFontSize)}px`,
    '--certificate-meta-font-size': `${clampCertificateMetaFontSize(resume.theme.certificateMetaFontSize)}px`,
    '--certificate-description-font-size': `${clampCertificateDescriptionFontSize(resume.theme.certificateDescriptionFontSize)}px`,
    '--skills-line-height': String(clampLineHeight(resume.theme.skillsLineHeight)),
    '--internship-summary-line-height': String(clampLineHeight(resume.theme.internshipSummaryLineHeight)),
    '--internship-highlights-line-height': String(clampLineHeight(resume.theme.internshipHighlightsLineHeight)),
    '--project-summary-line-height': String(clampLineHeight(resume.theme.projectSummaryLineHeight)),
    '--project-highlights-line-height': String(clampLineHeight(resume.theme.projectHighlightsLineHeight)),
    '--award-description-line-height': String(clampLineHeight(resume.theme.awardDescriptionLineHeight)),
    '--certificate-description-line-height': String(clampLineHeight(resume.theme.certificateDescriptionLineHeight)),
    '--self-summary-line-height': String(clampLineHeight(resume.theme.selfSummaryLineHeight)),
    '--self-summary-font-size': `${clampSelfSummaryFontSize(resume.theme.selfSummaryFontSize)}px`,
    '--section-gap': `${clampSectionGap(resume.theme.sectionGap)}px`,
    '--education-logo-size': `${clampEducationLogoSize(resume.theme.educationLogoSize)}px`,
  }))

  watchEffect(() => {
    if (typeof document === 'undefined') return
    const styleId = 'resume-theme-vars'
    let styleTag = document.getElementById(styleId)
    if (!styleTag) { styleTag = document.createElement('style'); styleTag.id = styleId; document.head.appendChild(styleTag) }
    const declarations = Object.entries(brandStyle.value).map(([key, value]) => `${key}: ${value};`).join('\n    ')
    styleTag.textContent = `:root {\n    ${declarations}\n}\n@media print { :root {\n    ${declarations}\n  } }`
  })

  restorePanelsState()
  bootstrapResumeAssets()

  return {
    resume, resumeAssets, currentAssetId, currentResumeAsset, assetsLoading, panels,
    photoUploadMessage, photoUploadError, educationLogoFeedback, jsonStatusMessage, jsonErrorMessage,
    actionStatusMessage, actionErrorMessage, exportWarningMessage, jsonInputRef, pageOverflow, pageHeight,
    storageBackend, brandStyle, togglePanel, expandAllPanels, collapseAllPanels, loadDemo, clearAll,
    saveDraft, restoreDraft, exportPdf, exportJson, triggerJsonImport, handleJsonImport,
    createResumeAssetVersion, selectResumeAsset, duplicateResumeAsset, updateResumeAssetMetadata,
    archiveResumeAsset, restoreResumeAsset, removeResumeAsset,
    addEducation, removeEducation, toggleEducationHidden, moveEducationUp, moveEducationDown,
    onEducationLogoChange, removeEducationLogo, addInternship, removeInternship, toggleInternshipHidden,
    moveInternshipUp, moveInternshipDown, onLogoChange, removeLogo, addResearchExperience,
    removeResearchExperience, toggleResearchExperienceHidden, moveResearchExperienceUp, moveResearchExperienceDown,
    addProject, removeProject, toggleProjectHidden, moveProjectUp, moveProjectDown, addStudentExperience,
    removeStudentExperience, toggleStudentExperienceHidden, moveStudentExperienceUp, moveStudentExperienceDown,
    addCustomImage, removeCustomImage, toggleCustomImageHidden, moveCustomImageUp, moveCustomImageDown,
    addAward, removeAward, toggleAwardHidden, moveAwardUp, moveAwardDown, addCertificate, removeCertificate,
    toggleCertificateHidden, moveCertificateUp, moveCertificateDown, updateLayoutOrder, onPhotoChange, removePhoto,
    onCustomImageChange, removeCustomImageFile, onPageOverflowChange,
  }
}
