// Lịch sử đọc của khách lưu trên trình duyệt; đã đăng nhập thì lưu trên server
const KEY = 'guest-history'
const MAX_ITEMS = 30

function read() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) ?? []
  } catch {
    return []
  }
}

export function guestHistory() {
  return read()
}

export function guestLastRead(comicId) {
  return read().find((item) => item.comicId === comicId) ?? null
}

export function saveGuestProgress(item) {
  try {
    const rest = read().filter((other) => other.comicId !== item.comicId)
    localStorage.setItem(KEY, JSON.stringify([{ ...item, lastReadAt: new Date().toISOString() }, ...rest].slice(0, MAX_ITEMS)))
  } catch {
    // Trình duyệt chặn lưu trữ thì bỏ qua, không ảnh hưởng việc đọc
  }
}

export function removeGuestProgress(comicId) {
  try {
    localStorage.setItem(KEY, JSON.stringify(read().filter((item) => item.comicId !== comicId)))
  } catch {
    // Như trên
  }
}
