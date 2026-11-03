export function compactNumber(value) {
  return new Intl.NumberFormat('vi-VN', { notation: 'compact', maximumFractionDigits: 1 }).format(value ?? 0)
}

export function fullNumber(value) {
  return new Intl.NumberFormat('vi-VN').format(value ?? 0)
}

export function timeAgo(iso) {
  if (!iso) return ''
  const seconds = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000)
  if (seconds < 60) return 'vừa xong'
  if (seconds < 3600) return `${Math.floor(seconds / 60)} phút trước`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} giờ trước`
  if (seconds < 2592000) return `${Math.floor(seconds / 86400)} ngày trước`
  return formatDate(iso)
}

export function formatDate(iso) {
  return iso ? new Date(iso).toLocaleDateString('vi-VN') : ''
}

export function formatDateTime(iso) {
  return iso ? new Date(iso).toLocaleString('vi-VN') : ''
}

/** 2.50 -> "2.5", 6.00 -> "6" */
export function chapterLabel(number) {
  return number == null ? '' : String(Number(number))
}

/** Tên chương riêng; tên chỉ lặp lại số chương ("Chương 3") thì không hiện. */
export function chapterTitleText(chapter) {
  const title = chapter.chapterTitle?.trim()
  return title && title !== `Chương ${chapterLabel(chapter.chapterNumber)}` ? title : ''
}

export const STATUS_LABEL = { ONGOING: 'Đang ra', COMPLETED: 'Hoàn thành', HIATUS: 'Tạm ngưng' }
