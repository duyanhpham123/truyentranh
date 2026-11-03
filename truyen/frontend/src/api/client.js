import axios from 'axios'

export const http = axios.create({ baseURL: '/api/v1', withCredentials: true })

// Access token chỉ giữ trong bộ nhớ; F5 thì lấy lại bằng refresh token trong cookie HttpOnly
let accessToken = null
let onSessionChange = () => {}

export function setAccessToken(token) {
  accessToken = token
}

export function setSessionListener(listener) {
  onSessionChange = listener
}

http.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

// Nhiều request cùng hết hạn token chỉ gọi refresh một lần
let refreshing = null

export function refreshSession() {
  refreshing ??= axios
    .post('/api/v1/auth/refresh', null, { withCredentials: true })
    .then(({ data }) => {
      accessToken = data.accessToken
      onSessionChange(data.user)
      return data
    })
    .catch((error) => {
      accessToken = null
      onSessionChange(null)
      throw error
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

http.interceptors.response.use(undefined, async (error) => {
  const { config, response } = error
  const isAuthCall = config?.url?.startsWith('/auth/')
  if (response?.status !== 401 || !config || config._retried || isAuthCall) throw error
  config._retried = true
  try {
    await refreshSession()
  } catch {
    // Phiên đã hết: request công khai vẫn chạy được khi không có token
    if (config.method !== 'get') throw error
  }
  delete config.headers.Authorization
  return http(config)
})

/** Lấy câu thông báo lỗi tiếng Việt từ phản hồi của backend. */
export function errorMessage(error) {
  const data = error?.response?.data
  if (data?.errors) return Object.values(data.errors)[0] ?? data.detail
  if (data?.detail) return data.detail
  if (!error?.response) return 'Không kết nối được máy chủ, hãy thử lại.'
  return 'Có lỗi xảy ra, hãy thử lại.'
}
