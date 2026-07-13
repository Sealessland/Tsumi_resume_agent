import { describe, expect, it } from 'vitest'
import validResume from '../../../../../contracts/fixtures/resume/valid-minimal-v13.json'
import invalidResume from '../../../../../contracts/fixtures/resume/invalid-missing-version.json'
import { createResumeEnvelope, validateResume } from './contracts'
import { createEmptyResume } from './templates'

describe('validateResume', () => {
  it('accepts the shared valid v13 fixture', () => {
    expect(validateResume(validResume)).toEqual({ valid: true, errors: [] })
  })

  it('rejects a fixture without version', () => {
    const result = validateResume(invalidResume)

    expect(result.valid).toBe(false)
    expect(result.errors).toContainEqual(expect.objectContaining({ keyword: 'required' }))
  })

  it('wraps the current local schema v12 data in a valid v13 server envelope', () => {
    const envelope = createResumeEnvelope(createEmptyResume(), {
      resumeId: 'res_fixture',
      version: 1,
    })

    expect(envelope.schemaVersion).toBe(13)
    expect(envelope).not.toHaveProperty('meta')
    expect(validateResume(envelope)).toEqual({ valid: true, errors: [] })
  })
})
