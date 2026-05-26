const TOKEN_KEY = 'paper_auth_token'

export function getToken() {
  return window.localStorage.getItem(TOKEN_KEY)
}

export function saveToken(token) {
  window.localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY)
}

export async function registerUser(payload) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function loginUser(payload) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function fetchCurrentUser() {
  return request('/api/auth/me', {
    headers: authHeaders()
  })
}

export async function fetchHealth() {
  return request('/api/health')
}

export async function pingAdmin() {
  return request('/api/admin/ping', {
    headers: authHeaders()
  })
}

export async function fetchTags() {
  return request('/api/tags')
}

export async function fetchPapers(params = {}) {
  const query = toQuery(params)
  return request(`/api/papers${query ? `?${query}` : ''}`)
}

export async function fetchPaperDetail(id) {
  return request(`/api/papers/${id}`)
}

export async function createPaper(payload) {
  return request('/api/papers', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(payload)
  })
}

export async function updatePaper(id, payload) {
  return request(`/api/admin/papers/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(payload)
  })
}

export async function softDeletePaper(id) {
  return request(`/api/admin/papers/${id}`, {
    method: 'DELETE',
    headers: authHeaders()
  })
}

export async function restorePaper(id) {
  return request(`/api/admin/papers/${id}/restore`, {
    method: 'POST',
    headers: authHeaders()
  })
}

export async function fetchDeletedPapers(params = {}) {
  const query = toQuery(params)
  return request(`/api/admin/papers/deleted${query ? `?${query}` : ''}`, {
    headers: authHeaders()
  })
}

export async function attemptPaperDownload(id) {
  return request(`/api/papers/${id}/download-attempt`, {
    method: 'POST',
    headers: authHeaders()
  })
}

export async function importArxivPapers(payload) {
  return request('/api/admin/papers/import/arxiv', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(payload)
  })
}

async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  })

  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json') ? await response.json() : null
  if (!response.ok) {
    throw new Error(data?.message || `请求失败: ${response.status}`)
  }
  return data
}

function authHeaders() {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function toQuery(params) {
  const query = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    query.set(key, String(value))
  })

  return query.toString()
}
