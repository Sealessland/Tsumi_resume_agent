let idSeed = 0
const DEFAULT_INTERNSHIP_LOGO_SIZE = 22
const DEFAULT_CUSTOM_IMAGE_WIDTH = 100

function clampLogoSize(value) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return DEFAULT_INTERNSHIP_LOGO_SIZE
  return Math.min(48, Math.max(14, Math.round(parsed)))
}

function clampCustomImageWidth(value) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return DEFAULT_CUSTOM_IMAGE_WIDTH
  return Math.min(100, Math.max(40, Math.round(parsed)))
}

export function createId(prefix) {
  idSeed += 1
  return `${prefix}-${Date.now()}-${idSeed}`
}

export function createEducationItem(partial = {}) {
  return {
    id: partial.id || createId('education'),
    hidden: Boolean(partial.hidden),
    logo: partial.logo || '',
    school: partial.school || '',
    degree: partial.degree || '',
    major: partial.major || '',
    studyPeriod: partial.studyPeriod || '',
  }
}

export function createPersonalDetailItem(partial = {}) {
  return {
    id: partial.id || createId('personal-detail'),
    label: partial.label || '',
    value: partial.value || '',
  }
}

export function createInternshipItem(partial = {}) {
  return {
    id: partial.id || createId('internship'),
    hidden: Boolean(partial.hidden),
    company: partial.company || '',
    department: partial.department || '',
    role: partial.role || '',
    period: partial.period || '',
    location: partial.location || '',
    summary: partial.summary || '',
    highlights: partial.highlights || '',
    logo: partial.logo || '',
    stripColor: partial.stripColor || '',
    logoSize: clampLogoSize(partial.logoSize),
  }
}

export function createProjectItem(partial = {}) {
  return {
    id: partial.id || createId('project'),
    hidden: Boolean(partial.hidden),
    name: partial.name || '',
    role: partial.role || '',
    period: partial.period || '',
    tags: partial.tags || '',
    summary: partial.summary || '',
    highlights: partial.highlights || '',
  }
}

export function createCustomImageItem(partial = {}) {
  return {
    id: partial.id || createId('custom-image'),
    hidden: Boolean(partial.hidden),
    title: partial.title || '自定义图片',
    subtitle: partial.subtitle || 'CUSTOM IMAGE',
    image: partial.image || '',
    alt: partial.alt || '',
    caption: partial.caption || '',
    widthPercent: clampCustomImageWidth(partial.widthPercent),
  }
}

export function createResearchExperienceItem(partial = {}) {
  return {
    id: partial.id || createId('research'),
    hidden: Boolean(partial.hidden),
    hidePaperInfo: Boolean(partial.hidePaperInfo),
    title: partial.title || '',
    lab: partial.lab || '',
    role: partial.role || '',
    supervisor: partial.supervisor || '',
    period: partial.period || '',
    paperTitle: partial.paperTitle || '',
    journal: partial.journal || '',
    publicationStatus: partial.publicationStatus || '',
    summary: partial.summary || '',
    highlights: partial.highlights || '',
  }
}

export function createStudentExperienceItem(partial = {}) {
  return {
    id: partial.id || createId('student'),
    hidden: Boolean(partial.hidden),
    organization: partial.organization || '',
    role: partial.role || '',
    period: partial.period || '',
    summary: partial.summary || '',
    highlights: partial.highlights || '',
  }
}

export function createAwardItem(partial = {}) {
  return {
    id: partial.id || createId('award'),
    hidden: Boolean(partial.hidden),
    name: partial.name || '',
    issuer: partial.issuer || '',
    level: partial.level || '',
    date: partial.date || '',
    description: partial.description || '',
  }
}

export function createCertificateItem(partial = {}) {
  return {
    id: partial.id || createId('certificate'),
    hidden: Boolean(partial.hidden),
    name: partial.name || '',
    issuer: partial.issuer || '',
    date: partial.date || '',
    credentialId: partial.credentialId || '',
    description: partial.description || '',
  }
}

export function createSectionVisibility(partial = {}) {
  return {
    profile: partial.profile !== false,
    education: partial.education !== false,
    skills: partial.skills !== false,
    internships: partial.internships !== false,
    researchExperiences: partial.researchExperiences !== false,
    projects: partial.projects !== false,
    studentExperiences: partial.studentExperiences !== false,
    customImages: partial.customImages !== false,
    awards: partial.awards !== false,
    certificates: partial.certificates !== false,
    selfSummary: partial.selfSummary !== false,
  }
}

export function createPanelsState() {
  return {
    layout: true,
    profile: true,
    education: true,
    skills: true,
    internship: true,
    research: true,
    project: true,
    student: true,
    awards: true,
    certificates: true,
    selfSummary: true,
    customImages: true,
    theme: true,
  }
}
