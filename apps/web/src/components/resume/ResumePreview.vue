<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Image as TImage } from 'tdesign-vue-next'
import { GovDanghui } from 'govui-vue3'

import { normalizePhotoConfig } from '../../modules/resume/photoConfig'
import { normalizeLayoutOrder } from '../../modules/resume/sections'
import { richText } from '../../modules/resume/richText'

const A4_HEIGHT_PX = 1123

const props = defineProps({
  resume: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['page-overflow-change'])
const pageRef = ref(null)
let resizeObserver = null

function splitLines(text = '') {
  return String(text)
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
}

function splitTags(text = '') {
  return String(text)
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

function cleanText(value = '') {
  return String(value).trim()
}

function getEducationDisplayParts(item = {}) {
  const parts = []
  const school = cleanText(item.school)
  const major = cleanText(item.major)
  const degree = cleanText(item.degree)
  const studyPeriod = cleanText(item.studyPeriod)

  if (school) parts.push({ key: 'school', text: school })
  if (major) parts.push({ key: 'major', text: major })
  if (degree) parts.push({ key: 'degree', text: degree })
  if (studyPeriod) parts.push({ key: 'studyPeriod', text: studyPeriod })

  return parts
}

function hasEducationContent(item = {}) {
  return getEducationDisplayParts(item).length > 0
}

function formatPersonalDetail(item = {}) {
  const label = cleanText(item.label)
  const value = cleanText(item.value)
  return label || value ? { label, value } : null
}

function formatWebsiteLabel(url = '') {
  return String(url).replace(/^https?:\/\//, '').replace(/\/$/, '')
}

function getWebsiteType(url = '') {
  const normalized = String(url).toLowerCase()
  if (normalized.includes('github.com')) return 'github'
  if (normalized.includes('gitee.com')) return 'gitee'
  return 'link'
}

function parseLogoSize(value) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return 22
  return Math.min(48, Math.max(14, Math.round(parsed)))
}

function getInternshipStripStyle(item) {
  if (!cleanText(item?.stripColor)) return null
  return { backgroundColor: item.stripColor }
}

function getInternshipLogoStyle(item) {
  return { height: `${parseLogoSize(item?.logoSize)}px` }
}

function clampCustomImageWidth(value) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return 100
  return Math.min(100, Math.max(40, Math.round(parsed)))
}

function getCustomImageStyle(item) {
  return { width: `${clampCustomImageWidth(item?.widthPercent)}%` }
}

function emitOverflowStatus() {
  const element = pageRef.value
  if (!element) return
  const height = Math.ceil(element.scrollHeight)
  emit('page-overflow-change', {
    overflow: height > A4_HEIGHT_PX,
    height,
  })
}

onMounted(() => {
  nextTick(emitOverflowStatus)
  if (typeof ResizeObserver !== 'undefined' && pageRef.value) {
    resizeObserver = new ResizeObserver(() => {
      emitOverflowStatus()
    })
    resizeObserver.observe(pageRef.value)
  }
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})

watch(
  () => props.resume,
  () => {
    nextTick(emitOverflowStatus)
  },
  { deep: true }
)

const hasPhoto = computed(() => Boolean(props.resume.profile.photo))
const photoConfig = computed(() => normalizePhotoConfig(props.resume.theme?.photoConfig || {}))
const photoStyle = computed(() => ({
  width: `${photoConfig.value.width}px`,
  height: `${photoConfig.value.height}px`,
}))
const resumeInfoStyle = computed(() => ({
  maxWidth: `calc(100% - ${photoConfig.value.width + 42}px)`,
}))

const sectionVisibility = computed(() => ({
  profile: props.resume.sectionVisibility?.profile !== false,
  education: props.resume.sectionVisibility?.education !== false,
  skills: props.resume.sectionVisibility?.skills !== false,
  internships: props.resume.sectionVisibility?.internships !== false,
  researchExperiences: props.resume.sectionVisibility?.researchExperiences !== false,
  projects: props.resume.sectionVisibility?.projects !== false,
  studentExperiences: props.resume.sectionVisibility?.studentExperiences !== false,
  customImages: props.resume.sectionVisibility?.customImages !== false,
  awards: props.resume.sectionVisibility?.awards !== false,
  certificates: props.resume.sectionVisibility?.certificates !== false,
  selfSummary: props.resume.sectionVisibility?.selfSummary !== false,
}))

const visibleEducations = computed(() =>
  (props.resume.educations || []).filter((item) => !item.hidden && hasEducationContent(item))
)
const showProfile = computed(() => sectionVisibility.value.profile)
const showEducationSection = computed(() => sectionVisibility.value.education && visibleEducations.value.length > 0)
const showHeader = computed(() => showProfile.value || showEducationSection.value)
const hasEducationLogo = computed(() => visibleEducations.value.some((item) => Boolean(item.logo)))

const skillLines = computed(() => splitLines(props.resume.skills))
const showSkills = computed(() => sectionVisibility.value.skills && skillLines.value.length > 0)

const visibleInternships = computed(() =>
  sectionVisibility.value.internships ? (props.resume.internships || []).filter((item) => !item.hidden) : []
)

const visibleResearchExperiences = computed(() =>
  sectionVisibility.value.researchExperiences
    ? (props.resume.researchExperiences || []).filter((item) => !item.hidden)
    : []
)

const visibleProjects = computed(() =>
  sectionVisibility.value.projects ? (props.resume.projects || []).filter((item) => !item.hidden) : []
)

const visibleStudentExperiences = computed(() =>
  sectionVisibility.value.studentExperiences
    ? (props.resume.studentExperiences || []).filter((item) => !item.hidden)
    : []
)

const visibleCustomImages = computed(() =>
  sectionVisibility.value.customImages
    ? (props.resume.customImages || []).filter((item) => !item.hidden && cleanText(item.image))
    : []
)

const visibleAwards = computed(() =>
  sectionVisibility.value.awards ? (props.resume.awards || []).filter((item) => !item.hidden) : []
)

const visibleCertificates = computed(() =>
  sectionVisibility.value.certificates ? (props.resume.certificates || []).filter((item) => !item.hidden) : []
)

const showSelfSummary = computed(() => {
  const content = cleanText(props.resume.selfSummary?.content || '')
  return sectionVisibility.value.selfSummary && Boolean(content) && !props.resume.selfSummary?.hidden
})

const educationFirst = computed(() => Boolean(props.resume.theme?.educationFirst))
const profileTitle = computed(() => cleanText(props.resume.profile.title))
const showPoliticalAffiliation = computed(() => {
  const affiliation = cleanText(props.resume.profile?.politicalAffiliation)
  return Boolean(props.resume.profile?.showPoliticalAffiliation) && Boolean(affiliation)
})
const politicalAffiliationLabel = computed(() => cleanText(props.resume.profile?.politicalAffiliation || ''))
const visiblePersonalDetails = computed(() =>
  (props.resume.profile?.personalDetails || [])
    .map((item) => formatPersonalDetail(item))
    .filter(Boolean)
)
const sectionOrderMap = computed(() => {
  const map = {}
  normalizeLayoutOrder(props.resume.layout?.order).forEach((id, index) => {
    map[id] = index + 1
  })
  return map
})

function getSectionOrder(id) {
  return sectionOrderMap.value[id] ?? 99
}

const contactItems = computed(() => {
  const items = []
  const phone = cleanText(props.resume.profile.phone)
  const email = cleanText(props.resume.profile.email)
  const website = cleanText(props.resume.profile.website)

  if (phone) items.push({ type: 'phone', text: phone, href: `tel:${phone}` })
  if (email) items.push({ type: 'email', text: email, href: `mailto:${email}` })
  if (website) items.push({ type: getWebsiteType(website), text: formatWebsiteLabel(website), href: website })
  return items
})

const hasAnyVisibleSection = computed(
  () =>
    showHeader.value ||
    showEducationSection.value ||
    showSkills.value ||
    visibleInternships.value.length > 0 ||
    visibleResearchExperiences.value.length > 0 ||
    visibleProjects.value.length > 0 ||
    visibleStudentExperiences.value.length > 0 ||
    visibleCustomImages.value.length > 0 ||
    visibleAwards.value.length > 0 ||
    visibleCertificates.value.length > 0 ||
    showSelfSummary.value
)
</script>

<template>
  <section class="glass-card flex justify-center overflow-x-auto p-2 sm:p-4 lg:p-5">
    <article id="resume-preview-page" ref="pageRef" class="resume-page resume-page--editorial">
      <div class="resume-flow">
        <header v-if="showHeader" class="resume-head resume-head--plain">
          <div :class="['resume-identity', hasPhoto ? 'resume-identity--with-photo' : 'resume-identity--no-photo']">
            <div class="resume-info" :class="hasPhoto ? 'text-left' : 'text-center'" :style="hasPhoto ? resumeInfoStyle : null">
              <template v-if="showProfile">
                <h2
                  class="font-bold tracking-tight text-[color:var(--brand-name)]"
                  style="font-family: var(--name-font); font-size: var(--name-font-size)"
                >
                  {{ resume.profile.name || '你的姓名' }}
                </h2>
                <p v-if="profileTitle" class="mt-0 text-[12px] font-semibold text-slate-700">
                  {{ profileTitle }}
                </p>
              </template>

              <div
                v-if="showEducationSection && educationFirst"
                class="mt-1 space-y-0 text-[12.5px] leading-5"
                :class="{ 'resume-education-block--with-logo': hasEducationLogo }"
              >
                <p
                  v-for="item in visibleEducations"
                  :key="item.id"
                  class="education-line"
                  :class="{ 'education-line--with-slot': item.logo }"
                >
                  <span v-if="item.logo" class="education-line-logo-slot">
                    <img
                      v-if="item.logo"
                      :src="item.logo"
                      alt="school logo"
                      class="education-school-logo"
                    />
                  </span>
                  <span class="education-line-content">
                    <template v-for="(part, index) in getEducationDisplayParts(item)" :key="`${item.id}-${part.key}`">
                      <span v-if="index > 0" class="mx-1 text-slate-300">/</span>
                      <span v-if="part.key === 'school'" class="education-school-wrap">
                        <strong
                          class="font-semibold text-[color:var(--brand-school)]"
                          style="font-family: var(--school-font); font-size: var(--school-font-size)"
                        >
                          {{ part.text }}
                        </strong>
                      </span>
                      <strong v-else-if="part.key === 'major'" class="font-semibold text-slate-800" style="font-size: var(--school-font-size)">
                        {{ part.text }}
                      </strong>
                      <span
                        v-else-if="part.key === 'degree'"
                        class="text-slate-600"
                        style="font-size: var(--school-font-size)"
                      >
                        {{ part.text }}
                      </span>
                      <span v-else class="text-slate-500">{{ part.text }}</span>
                    </template>
                  </span>
                </p>
              </div>

              <div
                v-if="showProfile && (visiblePersonalDetails.length || showPoliticalAffiliation)"
                class="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[12px] leading-5"
                :class="hasPhoto ? 'justify-start' : 'justify-center'"
              >
                <span v-if="showPoliticalAffiliation" class="political-affiliation-tag">
                  <GovDanghui
                    class="political-affiliation-icon"
                    type="red"
                    :width="14"
                  />
                  <span class="ml-1 text-red-600">{{ politicalAffiliationLabel }}</span>
                </span>
                <template v-for="(item, index) in visiblePersonalDetails" :key="`personal-${index}-${item.label}-${item.value}`">
                  <span v-if="index > 0 || showPoliticalAffiliation" class="text-slate-300">/</span>
                  <span>
                    <strong v-if="item.label" class="font-semibold text-[color:var(--brand)]">{{ item.label }}</strong>
                    <span v-if="item.label" class="text-[color:var(--brand)]">
                      {{ item.value ? ':' : '' }}
                    </span>
                    <span v-if="item.label" class="text-[color:var(--brand)]">{{ item.value ? ' ' : '' }}</span>
                    <span v-if="item.value" class="text-slate-500">{{ item.value }}</span>
                  </span>
                </template>
              </div>

              <div
                v-if="showProfile && contactItems.length"
                class="resume-contacts"
                :class="{ 'resume-contacts--with-education-logo': hasEducationLogo }"
              >
                <a
                  v-for="item in contactItems"
                  :key="`${item.type}-${item.text}`"
                  :href="item.href"
                  class="resume-contact"
                  target="_blank"
                  rel="noreferrer"
                >
                  <svg
                    v-if="item.type === 'phone'"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                    aria-hidden="true"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M22 16.92v3a2 2 0 0 1-2.18 2a19.8 19.8 0 0 1-8.63-3.07a19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.12 4.18A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.12.9.35 1.78.68 2.62a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.46-1.25a2 2 0 0 1 2.11-.45c.84.33 1.72.56 2.62.68A2 2 0 0 1 22 16.92Z"
                    />
                  </svg>
                  <svg
                    v-else-if="item.type === 'email'"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                    aria-hidden="true"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Z" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="m22 8l-8.97 5.7a2 2 0 0 1-2.06 0L2 8" />
                  </svg>
                  <svg
                    v-else-if="item.type === 'github'"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.7"
                    aria-hidden="true"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 19c-4.3 1.4-4.3-2.5-6-3m12 6v-3.9a3.4 3.4 0 0 0-.9-2.6c3-.3 6.2-1.5 6.2-6.8A5.3 5.3 0 0 0 19 4.8a4.9 4.9 0 0 0-.1-3.8s-1.1-.3-3.8 1.5a13.2 13.2 0 0 0-7 0C5.4.7 4.3 1 4.3 1a4.9 4.9 0 0 0-.1 3.8A5.3 5.3 0 0 0 2.9 8.7c0 5.3 3.2 6.5 6.2 6.8a3.4 3.4 0 0 0-.9 2.6V22" />
                  </svg>
                  <svg v-else-if="item.type === 'gitee'" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                    <path d="M3 6.8A3.8 3.8 0 0 1 6.8 3h10.4A3.8 3.8 0 0 1 21 6.8v10.4A3.8 3.8 0 0 1 17.2 21H6.8A3.8 3.8 0 0 1 3 17.2Zm4.8.2a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h7a1 1 0 0 0 0-2H8.8V13h5.3a1 1 0 1 0 0-2H8.8V9h6a1 1 0 0 0 0-2Z" />
                  </svg>
                  <svg
                    v-else
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                    aria-hidden="true"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="M18 13a5 5 0 0 0 0-7l-1-1a5 5 0 0 0-7 7" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 11a5 5 0 0 0 0 7l1 1a5 5 0 0 0 7-7" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M8 12h8" />
                  </svg>
                  <span>{{ item.text }}</span>
                </a>
              </div>

              <div
                v-if="showEducationSection && !educationFirst"
                class="mt-1 space-y-0 text-[12.5px] leading-5"
                :class="{ 'resume-education-block--with-logo': hasEducationLogo }"
              >
                <p
                  v-for="item in visibleEducations"
                  :key="`edu-tail-${item.id}`"
                  class="education-line"
                  :class="{ 'education-line--with-slot': item.logo }"
                >
                  <span v-if="item.logo" class="education-line-logo-slot">
                    <img
                      v-if="item.logo"
                      :src="item.logo"
                      alt="school logo"
                      class="education-school-logo"
                    />
                  </span>
                  <span class="education-line-content">
                    <template v-for="(part, index) in getEducationDisplayParts(item)" :key="`edu-tail-${item.id}-${part.key}`">
                      <span v-if="index > 0" class="mx-1 text-slate-300">/</span>
                      <span v-if="part.key === 'school'" class="education-school-wrap">
                        <strong
                          class="font-semibold text-[color:var(--brand-school)]"
                          style="font-family: var(--school-font); font-size: var(--school-font-size)"
                        >
                          {{ part.text }}
                        </strong>
                      </span>
                      <strong v-else-if="part.key === 'major'" class="font-semibold text-slate-800" style="font-size: var(--school-font-size)">
                        {{ part.text }}
                      </strong>
                      <span
                        v-else-if="part.key === 'degree'"
                        class="text-slate-600"
                        style="font-size: var(--school-font-size)"
                      >
                        {{ part.text }}
                      </span>
                      <span v-else class="text-slate-500">{{ part.text }}</span>
                    </template>
                  </span>
                </p>
              </div>
            </div>

            <TImage
              v-if="hasPhoto && showProfile"
              class="resume-photo resume-photo--plain"
              fit="cover"
              shape="round"
              :src="resume.profile.photo"
              alt="profile photo"
              :style="photoStyle"
            />
          </div>
        </header>

        <section v-if="showSkills" class="resume-section" :style="{ order: getSectionOrder('skills') }">
        <h3 class="resume-section-title">
          相关技能
          <span class="resume-section-subtitle">TECH STACK</span>
        </h3>
        <ul class="skill-list">
          <li v-for="(line, index) in skillLines" :key="`skill-${index}`" class="skill-line">
            <span class="skill-line-dot"></span>
            <span class="skill-line-text" v-html="richText(line)"></span>
          </li>
        </ul>
      </section>

        <section v-if="visibleInternships.length" class="resume-section" :style="{ order: getSectionOrder('internships') }">
        <h3 class="resume-section-title">
          实习经历
          <span class="resume-section-subtitle">INTERNSHIP EXPERIENCE</span>
        </h3>
        <div class="resume-list">
          <article v-for="item in visibleInternships" :key="item.id" class="resume-entry">
            <div class="entry-strip" :style="getInternshipStripStyle(item)">
              <div class="entry-strip-main">
                <p class="entry-time">{{ item.period || '时间段' }}</p>
                <p class="entry-brand">
                  <span>{{ item.company || '公司名称' }}</span>
                  <span v-if="cleanText(item.department)" class="entry-divider">|</span>
                  <span v-if="cleanText(item.department)">{{ item.department }}</span>
                  <span v-if="cleanText(item.location)" class="entry-location">· {{ item.location }}</span>
                </p>
                <p class="entry-role">{{ item.role || '岗位' }}</p>
              </div>
              <img v-if="item.logo" :src="item.logo" alt="实习公司 logo" class="entry-logo" :style="getInternshipLogoStyle(item)" />
            </div>
            <div class="entry-body">
              <p v-if="cleanText(item.summary)" class="entry-summary">
                <span class="entry-label">简介：</span>
                <span v-html="richText(item.summary)"></span>
              </p>
              <ul v-if="splitLines(item.highlights).length" class="entry-bullets">
                <li v-for="(line, index) in splitLines(item.highlights)" :key="`${item.id}-highlight-${index}`" class="entry-bullet">
                  <span class="entry-bullet-dot"></span>
                  <span v-html="richText(line)"></span>
                </li>
              </ul>
            </div>
          </article>
        </div>
      </section>

        <section
          v-if="visibleResearchExperiences.length"
          class="resume-section"
          :style="{ order: getSectionOrder('researchExperiences') }"
        >
        <h3 class="resume-section-title">
          科研经历
          <span class="resume-section-subtitle">RESEARCH EXPERIENCE</span>
        </h3>
        <div class="resume-list">
          <article v-for="item in visibleResearchExperiences" :key="item.id" class="project-entry">
            <div class="project-entry-head">
              <div class="project-entry-title-row">
                <p class="project-entry-name">{{ item.title || '科研课题' }}</p>
                <p class="project-entry-meta" v-if="cleanText(item.role) || cleanText(item.period)">
                  <span v-if="cleanText(item.role)">{{ item.role }}</span>
                  <span v-if="cleanText(item.role) && cleanText(item.period)"> / </span>
                  <span v-if="cleanText(item.period)">{{ item.period }}</span>
                </p>
              </div>
              <p class="plain-meta-sub" v-if="cleanText(item.lab) || cleanText(item.supervisor)">
                <span v-if="cleanText(item.lab)">{{ item.lab }}</span>
                <span v-if="cleanText(item.lab) && cleanText(item.supervisor)"> / </span>
                <span v-if="cleanText(item.supervisor)">{{ item.supervisor }}</span>
              </p>
              <p v-if="!item.hidePaperInfo && cleanText(item.paperTitle)" class="entry-summary">
                <span class="entry-label">论文：</span>
                <strong>{{ item.paperTitle }}</strong>
              </p>
              <div
                v-if="!item.hidePaperInfo && (cleanText(item.journal) || cleanText(item.publicationStatus))"
                class="project-tag-row"
              >
                <span v-if="cleanText(item.journal)" class="project-tag">{{ item.journal }}</span>
                <span v-if="cleanText(item.publicationStatus)" class="project-tag">{{ item.publicationStatus }}</span>
              </div>
            </div>
            <p v-if="cleanText(item.summary)" class="entry-summary">
              <span class="entry-label">简介：</span>
              <span v-html="richText(item.summary)"></span>
            </p>
            <ul v-if="splitLines(item.highlights).length" class="entry-bullets">
              <li
                v-for="(line, index) in splitLines(item.highlights)"
                :key="`${item.id}-research-highlight-${index}`"
                class="entry-bullet"
              >
                <span class="entry-bullet-dot"></span>
                <span v-html="richText(line)"></span>
              </li>
            </ul>
          </article>
        </div>
      </section>

        <section v-if="visibleProjects.length" class="resume-section" :style="{ order: getSectionOrder('projects') }">
        <h3 class="resume-section-title">
          项目经历
          <span class="resume-section-subtitle">PROJECTS</span>
        </h3>
        <div class="resume-list">
          <article v-for="item in visibleProjects" :key="item.id" class="project-entry">
            <div class="project-entry-head">
              <div class="project-entry-title-row">
                <p class="project-entry-name">{{ item.name || '项目名称' }}</p>
                <p class="project-entry-meta" v-if="cleanText(item.role) || cleanText(item.period)">
                  <span v-if="cleanText(item.role)">{{ item.role }}</span>
                  <span v-if="cleanText(item.role) && cleanText(item.period)"> / </span>
                  <span v-if="cleanText(item.period)">{{ item.period }}</span>
                </p>
              </div>
              <div v-if="splitTags(item.tags).length" class="project-tag-row">
                <span v-for="(tag, index) in splitTags(item.tags)" :key="`${item.id}-tag-${index}`" class="project-tag">
                  {{ tag }}
                </span>
              </div>
            </div>
            <p v-if="cleanText(item.summary)" class="project-summary">
              <span class="project-summary-label">简介：</span>
              <span v-html="richText(item.summary)"></span>
            </p>
            <ul v-if="splitLines(item.highlights).length" class="project-highlights">
              <li v-for="(line, index) in splitLines(item.highlights)" :key="`${item.id}-project-highlight-${index}`" class="project-highlight-item">
                <span class="project-highlight-dot"></span>
                <span v-html="richText(line)"></span>
              </li>
            </ul>
      </article>
    </div>
  </section>

    <section v-if="visibleStudentExperiences.length" class="resume-section" :style="{ order: getSectionOrder('studentExperiences') }">
      <h3 class="resume-section-title">
        学生经历
        <span class="resume-section-subtitle">STUDENT EXPERIENCE</span>
      </h3>
      <div class="resume-list">
        <article v-for="item in visibleStudentExperiences" :key="item.id" class="project-entry">
          <div class="project-entry-head">
            <div class="project-entry-title-row">
              <p class="project-entry-name" :style="{ fontSize: 'var(--student-name-font-size)' }">{{ item.organization || '组织/单位' }}</p>
              <p class="project-entry-meta" v-if="cleanText(item.role) || cleanText(item.period)" :style="{ fontSize: 'var(--student-meta-font-size)' }">
                <span v-if="cleanText(item.role)">{{ item.role }}</span>
                <span v-if="cleanText(item.role) && cleanText(item.period)"> / </span>
                <span v-if="cleanText(item.period)">{{ item.period }}</span>
              </p>
            </div>
          </div>
          <p v-if="cleanText(item.summary)" class="project-summary" :style="{ fontSize: 'var(--student-summary-font-size)' }">
            <span class="project-summary-label">简介：</span>
            <span v-html="richText(item.summary)"></span>
          </p>
          <ul v-if="splitLines(item.highlights).length" class="project-highlights">
            <li v-for="(line, index) in splitLines(item.highlights)" :key="`${item.id}-student-highlight-${index}`" class="project-highlight-item" :style="{ fontSize: 'var(--student-highlights-font-size)' }">
              <span class="project-highlight-dot"></span>
              <span v-html="richText(line)"></span>
            </li>
          </ul>
        </article>
      </div>
    </section>

    <section v-if="visibleCustomImages.length" class="resume-section" :style="{ order: getSectionOrder('customImages') }">
        <div class="custom-image-list">
          <article v-for="item in visibleCustomImages" :key="item.id" class="custom-image-entry">
            <h3 class="resume-section-title">
              {{ cleanText(item.title) || '图片展示' }}
              <span class="resume-section-subtitle">{{ cleanText(item.subtitle) || 'CUSTOM IMAGE' }}</span>
            </h3>
            <figure class="custom-image-figure">
              <img
                :src="item.image"
                :alt="cleanText(item.alt) || cleanText(item.title) || 'custom image'"
                class="custom-image-content"
                :style="getCustomImageStyle(item)"
              />
              <figcaption v-if="cleanText(item.caption)" class="custom-image-caption">
                {{ item.caption }}
              </figcaption>
            </figure>
          </article>
        </div>
      </section>

        <section v-if="visibleAwards.length" class="resume-section" :style="{ order: getSectionOrder('awards') }">
        <h3 class="resume-section-title">
          荣誉奖项
          <span class="resume-section-subtitle">AWARDS</span>
        </h3>
        <div class="resume-list resume-list--compact">
          <article v-for="item in visibleAwards" :key="item.id" class="plain-meta">
            <div class="plain-meta-head">
              <p class="plain-meta-title plain-meta-title--award">
                <span>{{ item.name || '奖项名称' }}</span>
                <span v-if="cleanText(item.level)" class="plain-meta-level plain-meta-level--award">{{ item.level }}</span>
                <span v-if="cleanText(item.issuer)" class="plain-meta-issuer plain-meta-issuer--award">· {{ item.issuer }}</span>
              </p>
              <p class="plain-meta-date plain-meta-date--award">{{ item.date || '获奖时间' }}</p>
            </div>
            <p v-if="cleanText(item.description)" class="plain-meta-desc plain-meta-desc--award" v-html="richText(item.description)"></p>
          </article>
        </div>
      </section>

        <section v-if="visibleCertificates.length" class="resume-section" :style="{ order: getSectionOrder('certificates') }">
        <h3 class="resume-section-title">
          证书
          <span class="resume-section-subtitle">CERTIFICATES</span>
        </h3>
        <div class="resume-list resume-list--compact">
          <article v-for="item in visibleCertificates" :key="item.id" class="plain-meta">
            <div class="plain-meta-head">
              <p class="plain-meta-title plain-meta-title--certificate">{{ item.name || '证书名称' }}</p>
              <p class="plain-meta-date plain-meta-date--certificate">{{ item.date || '获得时间' }}</p>
            </div>
            <p class="plain-meta-sub plain-meta-sub--certificate" v-if="cleanText(item.issuer) || cleanText(item.credentialId)">
              <span v-if="cleanText(item.issuer)">{{ item.issuer }}</span>
              <span v-if="cleanText(item.issuer) && cleanText(item.credentialId)"> / </span>
              <span v-if="cleanText(item.credentialId)">编号：{{ item.credentialId }}</span>
            </p>
            <p v-if="cleanText(item.description)" class="plain-meta-desc plain-meta-desc--certificate" v-html="richText(item.description)"></p>
          </article>
        </div>
      </section>

        <section v-if="showSelfSummary" class="resume-section" :style="{ order: getSectionOrder('selfSummary') }">
        <h3 class="resume-section-title">
          自我评价
          <span class="resume-section-subtitle">SUMMARY</span>
        </h3>
        <p class="summary-text" v-html="richText(resume.selfSummary.content)"></p>
        </section>

        <section v-if="!hasAnyVisibleSection" class="resume-section" :style="{ order: 999 }">
          <p class="summary-text text-slate-400">暂无可展示内容，请在左侧编辑区填写信息。</p>
        </section>
      </div>
    </article>
  </section>
</template>
