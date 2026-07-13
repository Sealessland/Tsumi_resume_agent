export const FIXED_HEADER_SECTION_IDS = [
  'profile',
  'education',
]

export const ORDERABLE_SECTION_IDS = [
  'skills',
  'internships',
  'researchExperiences',
  'projects',
  'studentExperiences',
  'customImages',
  'awards',
  'certificates',
  'selfSummary',
]

export const ORDERABLE_SECTION_LABELS = {
  skills: '技术栈',
  internships: '实习经历',
  researchExperiences: '科研经历',
  projects: '项目经历',
  studentExperiences: '学生经历',
  customImages: '图片展示',
  awards: '荣誉奖项',
  certificates: '证书',
  selfSummary: '自我评价',
}

export const SECTION_TO_PANEL_KEY = {
  profile: 'profile',
  education: 'education',
  skills: 'skills',
  internships: 'internship',
  researchExperiences: 'research',
  projects: 'project',
  studentExperiences: 'student',
  customImages: 'customImages',
  awards: 'awards',
  certificates: 'certificates',
  selfSummary: 'selfSummary',
}

export function normalizeLayoutOrder(order) {
  const source = Array.isArray(order) ? order : []
  const next = []

  source.forEach((id) => {
    if (ORDERABLE_SECTION_IDS.includes(id) && !next.includes(id)) {
      next.push(id)
    }
  })

  ORDERABLE_SECTION_IDS.forEach((id) => {
    if (!next.includes(id)) {
      next.push(id)
    }
  })

  return next
}

export function createDefaultLayout() {
  return {
    order: [...ORDERABLE_SECTION_IDS],
  }
}
