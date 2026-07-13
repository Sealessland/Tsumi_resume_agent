import { describe, expect, it } from 'vitest'
import validProposal from '../../../../../contracts/fixtures/patch/valid-paraphrase-proposal.json'
import invalidProposal from '../../../../../contracts/fixtures/patch/invalid-missing-evidence-proposal.json'
import validPatch from '../../../../../contracts/fixtures/patch/valid-paraphrase.json'
import invalidPatch from '../../../../../contracts/fixtures/patch/invalid-new-fact.json'
import validResume from '../../../../../contracts/fixtures/resume/valid-minimal-v13.json'
import invalidResume from '../../../../../contracts/fixtures/resume/invalid-missing-version.json'
import {
  createResumeEnvelope,
  validateResume,
  validateResumePatch,
  validateResumePatchProposal,
} from './contracts'
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

describe('validateResumePatchProposal', () => {
  it('accepts an evidence-referenced model proposal without server-owned fields', () => {
    expect(validateResumePatchProposal(validProposal)).toEqual({ valid: true, errors: [] })
    expect(validProposal).not.toHaveProperty('policyDecision')
    expect(validProposal).not.toHaveProperty('reviewStatus')
  })

  it('rejects a model proposal without evidence references', () => {
    expect(validateResumePatchProposal(invalidProposal).valid).toBe(false)
  })
})

describe('validateResumePatch', () => {
  it('accepts an evidence-backed paraphrase', () => {
    expect(validateResumePatch(validPatch)).toEqual({ valid: true, errors: [] })
  })

  it('rejects a patch containing a new unsupported fact', () => {
    expect(validateResumePatch(invalidPatch).valid).toBe(false)
  })
})
