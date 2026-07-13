import Ajv2020 from 'ajv/dist/2020'
import resumePatchSchema from '../../../../../contracts/resume-patch.schema.json'
import resumeSchema from '../../../../../contracts/resume.schema.json'
import { normalizeResumeData } from './normalize'

const ajv = new Ajv2020({ allErrors: true, strict: true })
const resumePatchValidator = ajv.compile(resumePatchSchema)
const resumeValidator = ajv.compile(resumeSchema)

function normalizeErrors(errors = []) {
  return errors.map((error) => ({
    instancePath: error.instancePath,
    schemaPath: error.schemaPath,
    keyword: error.keyword,
    message: error.message || 'Contract validation failed',
  }))
}

export function validateResume(value) {
  const valid = resumeValidator(value)
  return {
    valid,
    errors: valid ? [] : normalizeErrors(resumeValidator.errors),
  }
}

export function validateResumePatch(value) {
  const valid = resumePatchValidator(value)
  return {
    valid,
    errors: valid ? [] : normalizeErrors(resumePatchValidator.errors),
  }
}

export function createResumeEnvelope(source, { resumeId, version }) {
  const normalized = normalizeResumeData(source)
  const { meta: _localMeta, ...content } = normalized

  return {
    resumeId,
    version,
    schemaVersion: 13,
    ...content,
  }
}
