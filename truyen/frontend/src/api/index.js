import { http } from './client.js'

const data = (promise) => promise.then((response) => response.data)
const get = (url, params) => data(http.get(url, { params }))
const post = (url, body) => data(http.post(url, body))
const put = (url, body) => data(http.put(url, body))
const patch = (url, body) => data(http.patch(url, body))
const del = (url) => data(http.delete(url))

function upload(method, url, field, files) {
  const form = new FormData()
  for (const file of [].concat(files)) form.append(field, file)
  return data(http({ method, url, data: form }))
}

export const auth = {
  register: (body) => post('/auth/register', body),
  login: (body) => post('/auth/login', body),
  logout: () => post('/auth/logout'),
  forgot: (email) => post('/auth/forgot-password', { email }),
  reset: (body) => post('/auth/reset-password', body),
  verify: (token) => post('/auth/verify-email', { token }),
  resend: (email) => post('/auth/resend-verification', { email }),
}

export const me = {
  profile: () => get('/users/me'),
  update: (fullName) => patch('/users/me', { fullName }),
  password: (body) => put('/users/me/password', body),
  avatar: (file) => upload('put', '/users/me/avatar', 'file', file),
  history: (params) => get('/me/history', params),
  saveProgress: (comicId, chapterId, page) => put(`/me/history/${comicId}`, { chapterId, page }),
  deleteHistory: (comicId) => del(`/me/history/${comicId}`),
  follows: (params) => get('/me/follows', params),
  notifications: (params) => get('/me/notifications', params),
  unreadCount: () => get('/me/notifications/unread-count'),
  markRead: (id) => patch(`/me/notifications/${id}/read`),
  markAllRead: () => patch('/me/notifications/read-all'),
}

export const comics = {
  search: (params) => get('/comics', params),
  hot: (period) => get('/comics/hot', { period }),
  detail: (slug) => get(`/comics/${slug}`),
  myState: (id) => get(`/comics/${id}/me`),
  follow: (id) => post(`/comics/${id}/follow`),
  unfollow: (id) => del(`/comics/${id}/follow`),
  rating: (id) => get(`/comics/${id}/rating`),
  rate: (id, score) => put(`/comics/${id}/rating`, { score }),
  chapters: (slug) => get(`/comics/${slug}/chapters`),
  chapter: (slug, number) => get(`/comics/${slug}/chapters/${number}`),
  recordView: (chapterId) => post(`/chapters/${chapterId}/views`),
  comments: (id, params) => get(`/comics/${id}/comments`, params),
  comment: (id, body) => post(`/comics/${id}/comments`, body),
}

export const comments = {
  remove: (id) => del(`/comments/${id}`),
  like: (id) => put(`/comments/${id}/like`),
  unlike: (id) => del(`/comments/${id}/like`),
  report: (id, reason) => post(`/comments/${id}/reports`, { reason }),
}

export const catalog = {
  genres: () => get('/genres'),
  genre: (slug) => get(`/genres/${slug}`),
  authors: () => get('/authors'),
  author: (id) => get(`/authors/${id}`),
}

export const admin = {
  comics: (params) => get('/admin/comics', params),
  createComic: (body) => post('/admin/comics', body),
  updateComic: (id, body) => put(`/admin/comics/${id}`, body),
  deleteComic: (id) => del(`/admin/comics/${id}`),
  setHidden: (id, hidden) => patch(`/admin/comics/${id}/visibility`, { hidden }),
  cover: (id, file) => upload('put', `/admin/comics/${id}/cover`, 'file', file),

  chapters: (comicId) => get(`/admin/comics/${comicId}/chapters`),
  createChapter: (comicId, body) => post(`/admin/comics/${comicId}/chapters`, body),
  updateChapter: (id, body) => put(`/admin/chapters/${id}`, body),
  deleteChapter: (id) => del(`/admin/chapters/${id}`),
  uploadPages: (id, files) => upload('post', `/admin/chapters/${id}/pages`, 'files', files),
  reorderPages: (id, pageIds) => put(`/admin/chapters/${id}/pages`, { pageIds }),
  publish: (id) => post(`/admin/chapters/${id}/publish`),

  createGenre: (body) => post('/admin/genres', body),
  updateGenre: (id, body) => put(`/admin/genres/${id}`, body),
  deleteGenre: (id) => del(`/admin/genres/${id}`),

  createAuthor: (body) => post('/admin/authors', body),
  updateAuthor: (id, body) => put(`/admin/authors/${id}`, body),
  deleteAuthor: (id) => del(`/admin/authors/${id}`),

  users: (params) => get('/admin/users', params),
  setUserStatus: (id, status) => patch(`/admin/users/${id}/status`, { status }),
  setUserRole: (id, role) => patch(`/admin/users/${id}/role`, { role }),

  reports: (params) => get('/admin/comments/reports', params),
  resolveReport: (id, status) => patch(`/admin/comments/reports/${id}`, { status }),
  hideComment: (id) => del(`/admin/comments/${id}`),

  summary: () => get('/admin/dashboard/summary'),
  topComics: (params) => get('/admin/dashboard/top-comics', params),
  views: (params) => get('/admin/dashboard/views', params),
  newUsers: (params) => get('/admin/dashboard/new-users', params),
  topGenres: (params) => get('/admin/dashboard/top-genres', params),

  auditLogs: (params) => get('/admin/audit-logs', params),
  auditLog: (id) => get(`/admin/audit-logs/${id}`),
}
