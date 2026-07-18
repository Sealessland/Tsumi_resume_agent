export class AgentApiError extends Error {
  constructor(message, { status = 0, code = 'AGENT_API_ERROR', details = null } = {}) {
    super(message)
    this.name = 'AgentApiError'
    this.status = status
    this.code = code
    this.details = details
  }
}

function defaultIdempotencyKey() {
  const id = globalThis.crypto?.randomUUID?.()
    || `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return `web-${id}`
}

function pathSegment(value) {
  return encodeURIComponent(String(value || ''))
}

export function createAgentApi({
  fetchImpl = globalThis.fetch?.bind(globalThis),
  idempotencyKeyFactory = defaultIdempotencyKey,
} = {}) {
  if (!fetchImpl) throw new Error('Fetch API is unavailable')

  async function request(path, options = {}) {
    const headers = {
      Accept: 'application/json',
      ...options.headers,
    }
    if (options.body && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json'
    }

    let response
    try {
      response = await fetchImpl(path, { ...options, headers })
    } catch (error) {
      throw new AgentApiError('无法连接 Agent 后端，请确认 8080 服务已启动。', {
        code: 'AGENT_BACKEND_UNREACHABLE',
        details: error,
      })
    }

    const contentType = response.headers.get('content-type') || ''
    const payload = contentType.includes('json') ? await response.json() : null
    if (!response.ok) {
      throw new AgentApiError(
        payload?.detail || payload?.title || `Agent 请求失败（HTTP ${response.status}）`,
        {
          status: response.status,
          code: payload?.code || 'AGENT_API_ERROR',
          details: payload,
        },
      )
    }
    return payload
  }

  function mutation(path, body) {
    return request(path, {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKeyFactory() },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  }

  return {
    health() {
      return request('/actuator/health')
    },
    importResume(resume) {
      return request('/api/v1/resumes', {
        method: 'POST',
        body: JSON.stringify(resume),
      })
    },
    createTask(input) {
      return mutation('/api/v1/tasks', input)
    },
    getTask(taskId) {
      return request(`/api/v1/tasks/${pathSegment(taskId)}`)
    },
    getReviewSurface(taskId) {
      return request(`/api/v1/tasks/${pathSegment(taskId)}/review-surface`)
    },
    decidePatch(taskId, patchId, expectedBaseVersion, decision) {
      return mutation(
        `/api/v1/tasks/${pathSegment(taskId)}/patches/${pathSegment(patchId)}/decision`,
        { expectedBaseVersion, decision },
      )
    },
    editPatch(taskId, patchId, expectedBaseVersion, after) {
      return mutation(
        `/api/v1/tasks/${pathSegment(taskId)}/patches/${pathSegment(patchId)}/edit`,
        { expectedBaseVersion, after },
      )
    },
    mergeTask(taskId, expectedBaseVersion) {
      return mutation(`/api/v1/tasks/${pathSegment(taskId)}/merge`, { expectedBaseVersion })
    },
    cancelTask(taskId) {
      return mutation(`/api/v1/tasks/${pathSegment(taskId)}/cancel`)
    },
    retryTask(taskId) {
      return mutation(`/api/v1/tasks/${pathSegment(taskId)}/retry`)
    },
  }
}
