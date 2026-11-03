import { useEffect, useState } from 'react'

const KEY = 'theme'

function initialTheme() {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved) return saved
  } catch {
    // Không đọc được thì theo cài đặt hệ thống
  }
  return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** Gọi trước khi render để trang không nháy sai màu lúc mở. */
export function applyInitialTheme() {
  document.documentElement.dataset.theme = initialTheme()
}

export function useTheme() {
  const [theme, setTheme] = useState(initialTheme)
  useEffect(() => {
    document.documentElement.dataset.theme = theme
    try {
      localStorage.setItem(KEY, theme)
    } catch {
      // Bỏ qua
    }
  }, [theme])
  return [theme, () => setTheme((current) => (current === 'dark' ? 'light' : 'dark'))]
}
