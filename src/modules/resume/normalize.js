import { POLITICAL_AFFILIATION_DEFAULT, POLITICAL_AFFILIATION_OPTIONS, SCHEMA_VERSION } from './constants'
import {
  createAwardItem,
  createCertificateItem,
  createCustomImageItem,
  createEducationItem,
  createInternshipItem,
  createPersonalDetailItem,
  createProjectItem,
  createResearchExperienceItem,
  createStudentExperienceItem,
  createSectionVisibility,
} from './factories'
import { normalizePhotoConfig } from './photoConfig'
import { createDefaultLayout, normalizeLayoutOrder } from './sections'
import { createEmptyResume } from './templates'
import { normalizeNameFont, normalizeSchoolFont } from './nameFont'
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
} from './typography'

function clone(data) {
  return JSON.parse(JSON.stringify(data))
}

export function normalizeResumeData(source = {}) {
  const normalized = clone(createEmptyResume())

  if (source.profile && typeof source.profile === 'object') {
    Object.assign(normalized.profile, source.profile)
    normalized.profile.personalDetails = Array.isArray(source.profile.personalDetails)
      ? source.profile.personalDetails.map((item) => createPersonalDetailItem(item))
      : []

    const validPoliticalValues = POLITICAL_AFFILIATION_OPTIONS.map((o) => o.value)
    normalized.profile.politicalAffiliation = validPoliticalValues.includes(source.profile.politicalAffiliation)
      ? source.profile.politicalAffiliation
      : POLITICAL_AFFILIATION_DEFAULT
    normalized.profile.showPoliticalAffiliation = Boolean(source.profile.showPoliticalAffiliation)
  }

  const sourceEducations = Array.isArray(source.educations)
    ? source.educations
    : source.education && typeof source.education === 'object'
      ? [source.education]
      : []

  if (sourceEducations.length) {
    normalized.educations = sourceEducations.map((item) => createEducationItem(item))
  } else if (source.profile && typeof source.profile === 'object') {
    const legacySchool = source.profile.school || ''
    const legacyMajor = source.profile.major || ''
    const legacyStudyPeriod = source.profile.eduTime || ''
    if (legacySchool || legacyMajor || legacyStudyPeriod) {
      normalized.educations = [
        createEducationItem({
          school: legacySchool,
          major: legacyMajor,
          studyPeriod: legacyStudyPeriod,
        }),
      ]
    }
  }

  if (Object.prototype.hasOwnProperty.call(source, 'skills')) {
    normalized.skills = String(source.skills || '')
  }

  const sourceInternships = Array.isArray(source.internships)
    ? source.internships
    : source.internship && typeof source.internship === 'object'
      ? [source.internship]
      : []
  normalized.internships = sourceInternships.map((item) => createInternshipItem(item))

  const sourceProjects = Array.isArray(source.projects)
    ? source.projects
    : source.project && typeof source.project === 'object'
      ? [source.project]
      : []
  normalized.projects = sourceProjects.map((item) => createProjectItem(item))

  const sourceStudentExperiences = Array.isArray(source.studentExperiences)
    ? source.studentExperiences
    : []
  normalized.studentExperiences = sourceStudentExperiences.map((item) => createStudentExperienceItem(item))

  normalized.customImages = Array.isArray(source.customImages)
    ? source.customImages.map((item) => createCustomImageItem(item))
    : []

  const sourceResearchExperiences = Array.isArray(source.researchExperiences)
    ? source.researchExperiences
    : source.researchExperience && typeof source.researchExperience === 'object'
      ? [source.researchExperience]
      : []
  normalized.researchExperiences = sourceResearchExperiences.map((item) => createResearchExperienceItem(item))

  const sourceAwards = Array.isArray(source.awards)
    ? source.awards
    : source.award && typeof source.award === 'object'
      ? [source.award]
      : []
  normalized.awards = sourceAwards.map((item) => createAwardItem(item))

  const sourceCertificates = Array.isArray(source.certificates)
    ? source.certificates
    : source.certificate && typeof source.certificate === 'object'
      ? [source.certificate]
      : []
  normalized.certificates = sourceCertificates.map((item) => createCertificateItem(item))

  if (source.selfSummary && typeof source.selfSummary === 'object') {
    normalized.selfSummary.content = String(source.selfSummary.content || '')
    normalized.selfSummary.hidden = Boolean(source.selfSummary.hidden)
  } else if (source.summary && typeof source.summary === 'object') {
    normalized.selfSummary.content = String(source.summary.content || '')
  }

  normalized.sectionVisibility = createSectionVisibility(source.sectionVisibility || {})
  normalized.layout = createDefaultLayout()
  normalized.layout.order = normalizeLayoutOrder(source.layout?.order)

  if (source.theme && typeof source.theme === 'object') {
    Object.assign(normalized.theme, source.theme)
  }
  normalized.theme.nameColor = String(normalized.theme.nameColor || '')
  normalized.theme.schoolColor = String(normalized.theme.schoolColor || '')
  normalized.theme.nameFont = normalizeNameFont(normalized.theme.nameFont)
  normalized.theme.nameFontSize = clampNameFontSize(normalized.theme.nameFontSize)
  normalized.theme.schoolFont = normalizeSchoolFont(normalized.theme.schoolFont)
  normalized.theme.schoolFontSize = clampSchoolFontSize(normalized.theme.schoolFontSize)
  normalized.theme.skillsFontSize = clampSkillsFontSize(normalized.theme.skillsFontSize)
  normalized.theme.internshipSummaryFontSize = clampInternshipSummaryFontSize(normalized.theme.internshipSummaryFontSize)
  normalized.theme.internshipHighlightsFontSize = clampInternshipHighlightsFontSize(normalized.theme.internshipHighlightsFontSize)
  normalized.theme.internshipTimeFontSize = clampInternshipTimeFontSize(normalized.theme.internshipTimeFontSize)
  normalized.theme.internshipCompanyMetaFontSize = clampInternshipCompanyMetaFontSize(
    normalized.theme.internshipCompanyMetaFontSize
  )
  normalized.theme.internshipRoleFontSize = clampInternshipRoleFontSize(normalized.theme.internshipRoleFontSize)
  normalized.theme.projectSummaryFontSize = clampProjectSummaryFontSize(normalized.theme.projectSummaryFontSize)
  normalized.theme.projectHighlightsFontSize = clampProjectHighlightsFontSize(normalized.theme.projectHighlightsFontSize)
  normalized.theme.projectNameFontSize = clampProjectNameFontSize(normalized.theme.projectNameFontSize)
  normalized.theme.projectMetaFontSize = clampProjectMetaFontSize(normalized.theme.projectMetaFontSize)
  normalized.theme.projectTagFontSize = clampProjectTagFontSize(normalized.theme.projectTagFontSize)
  normalized.theme.studentNameFontSize = clampStudentNameFontSize(normalized.theme.studentNameFontSize)
  normalized.theme.studentMetaFontSize = clampStudentMetaFontSize(normalized.theme.studentMetaFontSize)
  normalized.theme.studentSummaryFontSize = clampStudentSummaryFontSize(normalized.theme.studentSummaryFontSize)
  normalized.theme.studentHighlightsFontSize = clampStudentHighlightsFontSize(normalized.theme.studentHighlightsFontSize)
  normalized.theme.awardTitleFontSize = clampAwardTitleFontSize(normalized.theme.awardTitleFontSize)
  normalized.theme.awardMetaFontSize = clampAwardMetaFontSize(normalized.theme.awardMetaFontSize)
  normalized.theme.awardDescriptionFontSize = clampAwardDescriptionFontSize(normalized.theme.awardDescriptionFontSize)
  normalized.theme.certificateTitleFontSize = clampCertificateTitleFontSize(normalized.theme.certificateTitleFontSize)
  normalized.theme.certificateMetaFontSize = clampCertificateMetaFontSize(
    normalized.theme.certificateMetaFontSize
  )
  normalized.theme.certificateDescriptionFontSize = clampCertificateDescriptionFontSize(normalized.theme.certificateDescriptionFontSize)
  normalized.theme.selfSummaryFontSize = clampSelfSummaryFontSize(normalized.theme.selfSummaryFontSize)
  normalized.theme.skillsLineHeight = clampLineHeight(normalized.theme.skillsLineHeight)
  normalized.theme.internshipSummaryLineHeight = clampLineHeight(normalized.theme.internshipSummaryLineHeight)
  normalized.theme.internshipHighlightsLineHeight = clampLineHeight(normalized.theme.internshipHighlightsLineHeight)
  normalized.theme.projectSummaryLineHeight = clampLineHeight(normalized.theme.projectSummaryLineHeight)
  normalized.theme.projectHighlightsLineHeight = clampLineHeight(normalized.theme.projectHighlightsLineHeight)
  normalized.theme.awardDescriptionLineHeight = clampLineHeight(normalized.theme.awardDescriptionLineHeight)
  normalized.theme.certificateDescriptionLineHeight = clampLineHeight(normalized.theme.certificateDescriptionLineHeight)
  normalized.theme.selfSummaryLineHeight = clampLineHeight(normalized.theme.selfSummaryLineHeight)
  normalized.theme.sectionGap = clampSectionGap(normalized.theme.sectionGap)
  normalized.theme.educationLogoSize = clampEducationLogoSize(normalized.theme.educationLogoSize)
  normalized.theme.boldMajor = Boolean(normalized.theme.boldMajor)
  normalized.theme.educationFirst = normalized.theme.educationFirst !== false
  normalized.theme.photoConfig = normalizePhotoConfig(normalized.theme.photoConfig)

  normalized.meta.schemaVersion = Number(source.meta?.schemaVersion || SCHEMA_VERSION)

  if (!normalized.profile.photo) {
    normalized.profile.photoMeta = null
  }

  return normalized
}
