export const DEFAULT_NAME_FONT_SIZE = 18
export const DEFAULT_SCHOOL_FONT_SIZE = 13
export const DEFAULT_SKILLS_FONT_SIZE = 12.5
export const DEFAULT_INTERNSHIP_SUMMARY_FONT_SIZE = 12.5
export const DEFAULT_INTERNSHIP_HIGHLIGHTS_FONT_SIZE = 12.5
export const DEFAULT_INTERNSHIP_TIME_FONT_SIZE = 12
export const DEFAULT_INTERNSHIP_COMPANY_META_FONT_SIZE = 13
export const DEFAULT_INTERNSHIP_ROLE_FONT_SIZE = 13
export const DEFAULT_PROJECT_SUMMARY_FONT_SIZE = 12.5
export const DEFAULT_PROJECT_HIGHLIGHTS_FONT_SIZE = 12.5
export const DEFAULT_PROJECT_NAME_FONT_SIZE = 13
export const DEFAULT_PROJECT_META_FONT_SIZE = 12
export const DEFAULT_PROJECT_TAG_FONT_SIZE = 11
export const DEFAULT_STUDENT_NAME_FONT_SIZE = 13
export const DEFAULT_STUDENT_META_FONT_SIZE = 12
export const DEFAULT_STUDENT_SUMMARY_FONT_SIZE = 12.5
export const DEFAULT_STUDENT_HIGHLIGHTS_FONT_SIZE = 12.5
export const DEFAULT_AWARD_TITLE_FONT_SIZE = 13
export const DEFAULT_AWARD_META_FONT_SIZE = 12
export const DEFAULT_AWARD_DESCRIPTION_FONT_SIZE = 12.5
export const DEFAULT_CERTIFICATE_TITLE_FONT_SIZE = 13
export const DEFAULT_CERTIFICATE_META_FONT_SIZE = 12
export const DEFAULT_CERTIFICATE_DESCRIPTION_FONT_SIZE = 12.5
export const DEFAULT_SELF_SUMMARY_FONT_SIZE = 12.5

export const NAME_FONT_SIZE_MIN = 14
export const NAME_FONT_SIZE_MAX = 32

export const SCHOOL_FONT_SIZE_MIN = 11
export const SCHOOL_FONT_SIZE_MAX = 22

export const DEFAULT_EDUCATION_LOGO_SIZE = 18
export const EDUCATION_LOGO_SIZE_MIN = 14
export const EDUCATION_LOGO_SIZE_MAX = 48

export const CONTENT_FONT_SIZE_MIN = 11
export const CONTENT_FONT_SIZE_MAX = 16
export const META_FONT_SIZE_MIN = 11
export const META_FONT_SIZE_MAX = 16
export const EMPHASIS_FONT_SIZE_MIN = 12
export const EMPHASIS_FONT_SIZE_MAX = 18

function clampSize(value, min, max, fallback) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return fallback
  return Math.min(max, Math.max(min, Math.round(parsed * 10) / 10))
}

export const DEFAULT_LINE_HEIGHT = 1.6
export const LINE_HEIGHT_MIN = 1.2
export const LINE_HEIGHT_MAX = 2.2

export const DEFAULT_SECTION_GAP = 10
export const SECTION_GAP_MIN = 4
export const SECTION_GAP_MAX = 32

export function clampLineHeight(value, fallback = DEFAULT_LINE_HEIGHT) {
  if (value === null || value === undefined || value === '') return fallback
  return clampSize(value, LINE_HEIGHT_MIN, LINE_HEIGHT_MAX, fallback)
}

export function clampSectionGap(value, fallback = DEFAULT_SECTION_GAP) {
  if (value === null || value === undefined || value === '') return fallback
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return fallback
  return Math.min(SECTION_GAP_MAX, Math.max(SECTION_GAP_MIN, Math.round(parsed)))
}

export function clampEducationLogoSize(value, fallback = DEFAULT_EDUCATION_LOGO_SIZE) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return fallback
  return Math.min(EDUCATION_LOGO_SIZE_MAX, Math.max(EDUCATION_LOGO_SIZE_MIN, Math.round(parsed)))
}
export function clampNameFontSize(value, fallback = DEFAULT_NAME_FONT_SIZE) {
  return clampSize(value, NAME_FONT_SIZE_MIN, NAME_FONT_SIZE_MAX, fallback)
}

export function clampSchoolFontSize(value, fallback = DEFAULT_SCHOOL_FONT_SIZE) {
  return clampSize(value, SCHOOL_FONT_SIZE_MIN, SCHOOL_FONT_SIZE_MAX, fallback)
}

export function clampSkillsFontSize(value, fallback = DEFAULT_SKILLS_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}

export function clampInternshipSummaryFontSize(value, fallback = DEFAULT_INTERNSHIP_SUMMARY_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}

export function clampInternshipHighlightsFontSize(value, fallback = DEFAULT_INTERNSHIP_HIGHLIGHTS_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}

export function clampInternshipTimeFontSize(value, fallback = DEFAULT_INTERNSHIP_TIME_FONT_SIZE) {
  return clampSize(value, META_FONT_SIZE_MIN, META_FONT_SIZE_MAX, fallback)
}

export function clampInternshipCompanyMetaFontSize(
  value,
  fallback = DEFAULT_INTERNSHIP_COMPANY_META_FONT_SIZE
) {
  return clampSize(value, META_FONT_SIZE_MIN, META_FONT_SIZE_MAX, fallback)
}

export function clampInternshipRoleFontSize(value, fallback = DEFAULT_INTERNSHIP_ROLE_FONT_SIZE) {
  return clampSize(value, EMPHASIS_FONT_SIZE_MIN, EMPHASIS_FONT_SIZE_MAX, fallback)
}

export function clampProjectSummaryFontSize(value, fallback = DEFAULT_PROJECT_SUMMARY_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}

export function clampProjectHighlightsFontSize(value, fallback = DEFAULT_PROJECT_HIGHLIGHTS_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}

export function clampProjectNameFontSize(value, fallback = DEFAULT_PROJECT_NAME_FONT_SIZE) {
  return clampSize(value, EMPHASIS_FONT_SIZE_MIN, EMPHASIS_FONT_SIZE_MAX, fallback)
}

export function clampProjectMetaFontSize(value, fallback = DEFAULT_PROJECT_META_FONT_SIZE) {
  return clampSize(value, META_FONT_SIZE_MIN, META_FONT_SIZE_MAX, fallback)
}

export function clampProjectTagFontSize(value, fallback = DEFAULT_PROJECT_TAG_FONT_SIZE) {
  return clampSize(value, META_FONT_SIZE_MIN, META_FONT_SIZE_MAX, fallback)
}

export function clampStudentNameFontSize(value, fallback = DEFAULT_STUDENT_NAME_FONT_SIZE) {
  return clampSize(value, EMPHASIS_FONT_SIZE_MIN, EMPHASIS_FONT_SIZE_MAX, fallback)
}

export function clampStudentMetaFontSize(value, fallback = DEFAULT_STUDENT_META_FONT_SIZE) {
  return clampSize(value, META_FONT_SIZE_MIN, META_FONT_SIZE_MAX, fallback)
}

export function clampStudentSummaryFontSize(value, fallback = DEFAULT_STUDENT_SUMMARY_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}

export function clampStudentHighlightsFontSize(value, fallback = DEFAULT_STUDENT_HIGHLIGHTS_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}

export function clampAwardTitleFontSize(value, fallback = DEFAULT_AWARD_TITLE_FONT_SIZE) {
  return clampSize(value, EMPHASIS_FONT_SIZE_MIN, EMPHASIS_FONT_SIZE_MAX, fallback)
}

export function clampAwardMetaFontSize(value, fallback = DEFAULT_AWARD_META_FONT_SIZE) {
  return clampSize(value, META_FONT_SIZE_MIN, META_FONT_SIZE_MAX, fallback)
}

export function clampAwardDescriptionFontSize(value, fallback = DEFAULT_AWARD_DESCRIPTION_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}

export function clampCertificateTitleFontSize(value, fallback = DEFAULT_CERTIFICATE_TITLE_FONT_SIZE) {
  return clampSize(value, EMPHASIS_FONT_SIZE_MIN, EMPHASIS_FONT_SIZE_MAX, fallback)
}

export function clampCertificateMetaFontSize(value, fallback = DEFAULT_CERTIFICATE_META_FONT_SIZE) {
  return clampSize(value, META_FONT_SIZE_MIN, META_FONT_SIZE_MAX, fallback)
}

export function clampCertificateDescriptionFontSize(value, fallback = DEFAULT_CERTIFICATE_DESCRIPTION_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}

export function clampSelfSummaryFontSize(value, fallback = DEFAULT_SELF_SUMMARY_FONT_SIZE) {
  return clampSize(value, CONTENT_FONT_SIZE_MIN, CONTENT_FONT_SIZE_MAX, fallback)
}
