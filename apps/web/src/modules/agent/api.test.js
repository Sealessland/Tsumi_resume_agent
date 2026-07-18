import { describe, expect, it, vi } from 'vitest'
import { createAgentApi } from './api'

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('createAgentApi', () => {
  it('imports the current resume and creates an idempotent task', async () => {
    const fetchImpl = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ resumeId: 'res_ui', version: 1 }, 201))
      .mockResolvedValueOnce(jsonResponse({ taskId: 'task_ui', status: 'CREATED' }, 202))
    const api = createAgentApi({ fetchImpl, idempotencyKeyFactory: () => 'key-fixed' })
    const resume = { resumeId: 'res_ui', version: 1, schemaVersion: 13 }

    await api.importResume(resume)
    await api.createTask({ resumeId: 'res_ui', baseVersion: 1, jobDescription: 'Java Agent' })

    expect(fetchImpl).toHaveBeenNthCalledWith(1, '/api/v1/resumes', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(resume),
    }))
    expect(fetchImpl).toHaveBeenNthCalledWith(2, '/api/v1/tasks', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({ 'Idempotency-Key': 'key-fixed' }),
      body: JSON.stringify({ resumeId: 'res_ui', baseVersion: 1, jobDescription: 'Java Agent' }),
    }))
  })

  it('loads task and review state', async () => {
    const fetchImpl = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ taskId: 'task_ui', status: 'REVIEW_READY' }))
      .mockResolvedValueOnce(jsonResponse({ taskId: 'task_ui', patches: [] }))
    const api = createAgentApi({ fetchImpl })

    await api.getTask('task_ui')
    await api.getReviewSurface('task_ui')

    expect(fetchImpl).toHaveBeenNthCalledWith(1, '/api/v1/tasks/task_ui', expect.any(Object))
    expect(fetchImpl).toHaveBeenNthCalledWith(
      2,
      '/api/v1/tasks/task_ui/review-surface',
      expect.objectContaining({ headers: expect.objectContaining({ Accept: 'application/json' }) }),
    )
  })

  it('sends human decisions, edits and merges with separate idempotency keys', async () => {
    let keyIndex = 0
    const fetchImpl = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ patchId: 'rp_1', reviewStatus: 'ACCEPTED' }))
      .mockResolvedValueOnce(jsonResponse({ patchId: 'rp_1_r2', reviewStatus: 'PENDING' }))
      .mockResolvedValueOnce(jsonResponse({ resumeId: 'res_ui', version: 2 }, 201))
    const api = createAgentApi({
      fetchImpl,
      idempotencyKeyFactory: () => `key-${++keyIndex}`,
    })

    await api.decidePatch('task_ui', 'rp_1', 1, 'ACCEPTED')
    await api.editPatch('task_ui', 'rp_1', 1, 'rewritten')
    await api.mergeTask('task_ui', 1)

    expect(fetchImpl).toHaveBeenNthCalledWith(
      1,
      '/api/v1/tasks/task_ui/patches/rp_1/decision',
      expect.objectContaining({
        headers: expect.objectContaining({ 'Idempotency-Key': 'key-1' }),
        body: JSON.stringify({ expectedBaseVersion: 1, decision: 'ACCEPTED' }),
      }),
    )
    expect(fetchImpl).toHaveBeenNthCalledWith(
      2,
      '/api/v1/tasks/task_ui/patches/rp_1/edit',
      expect.objectContaining({ body: JSON.stringify({ expectedBaseVersion: 1, after: 'rewritten' }) }),
    )
    expect(fetchImpl).toHaveBeenNthCalledWith(
      3,
      '/api/v1/tasks/task_ui/merge',
      expect.objectContaining({ body: JSON.stringify({ expectedBaseVersion: 1 }) }),
    )
  })

  it('exposes RFC 7807 details on request failures', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(jsonResponse({
      code: 'POLICY_REJECTED',
      detail: 'Unsupported claim',
    }, 422))
    const api = createAgentApi({ fetchImpl })

    await expect(api.getTask('task_bad')).rejects.toMatchObject({
      message: 'Unsupported claim',
      code: 'POLICY_REJECTED',
      status: 422,
    })
  })
})
