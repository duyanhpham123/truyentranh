import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import Icon from './Icon.jsx'
import { errorMessage } from '../api/client.js'

export function Loading({ label = 'Đang tải...' }) {
  return (
    <div className="loading" role="status">
      <span className="loading-burst" aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}

export function ErrorBox({ error, onRetry }) {
  return (
    <div className="empty empty-error" role="alert">
      <strong>Ối! Không tải được dữ liệu.</strong>
      <p>{errorMessage(error)}</p>
      {onRetry && (
        <button className="btn" onClick={onRetry}>
          Thử lại
        </button>
      )}
    </div>
  )
}

export function Empty({ title, children, action }) {
  return (
    <div className="empty">
      <strong>{title}</strong>
      {children && <p>{children}</p>}
      {action}
    </div>
  )
}

export function SectionTitle({ children, icon, action }) {
  return (
    <div className="section-title">
      <h2>
        {icon && <Icon name={icon} size={24} />}
        {children}
      </h2>
      {action}
    </div>
  )
}

export function Field({ label, error, hint, children }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {hint && !error && <span className="field-hint">{hint}</span>}
      {error && <span className="field-error">{error}</span>}
    </label>
  )
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null
  const pages = []
  for (let i = 0; i < totalPages; i++) {
    if (i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1) pages.push(i)
    else if (pages[pages.length - 1] !== '...') pages.push('...')
  }
  return (
    <nav className="pagination" aria-label="Phân trang">
      <button className="page-btn" disabled={page === 0} onClick={() => onChange(page - 1)} aria-label="Trang trước">
        <Icon name="left" size={16} />
      </button>
      {pages.map((value, index) =>
        value === '...' ? (
          <span key={`gap-${index}`} className="page-gap">
            …
          </span>
        ) : (
          <button
            key={value}
            className={`page-btn ${value === page ? 'is-active' : ''}`}
            aria-current={value === page ? 'page' : undefined}
            onClick={() => onChange(value)}
          >
            {value + 1}
          </button>
        ),
      )}
      <button className="page-btn" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)} aria-label="Trang sau">
        <Icon name="right" size={16} />
      </button>
    </nav>
  )
}

export function Stars({ value = 0, onRate, size = 18 }) {
  const rounded = Math.round(Number(value))
  return (
    <span className={`stars ${onRate ? 'stars-input' : ''}`}>
      {[1, 2, 3, 4, 5].map((star) =>
        onRate ? (
          <button key={star} type="button" onClick={() => onRate(star)} aria-label={`Chấm ${star} sao`} className={star <= rounded ? 'is-on' : ''}>
            <Icon name="star" size={size} filled={star <= rounded} />
          </button>
        ) : (
          <span key={star} className={star <= rounded ? 'is-on' : ''}>
            <Icon name="star" size={size} filled={star <= rounded} />
          </span>
        ),
      )}
    </span>
  )
}

export function Modal({ title, onClose, children }) {
  const ref = useRef(null)
  useEffect(() => {
    const dialog = ref.current
    dialog.showModal()
    return () => dialog.close()
  }, [])
  return (
    <dialog ref={ref} className="modal" onCancel={onClose}>
      <div className="modal-head">
        <h3>{title}</h3>
        <button className="icon-btn" onClick={onClose} aria-label="Đóng">
          <Icon name="x" />
        </button>
      </div>
      <div className="modal-body">{children}</div>
    </dialog>
  )
}

export function Avatar({ url, name, size = 40 }) {
  const initial = (name ?? '?').trim().charAt(0).toLocaleUpperCase('vi-VN')
  return (
    <span className="avatar" style={{ width: size, height: size, fontSize: size * 0.42 }}>
      {url ? <img src={url} alt="" /> : initial}
    </span>
  )
}

export function LoginPrompt({ children = 'để dùng tính năng này' }) {
  return (
    <p className="login-prompt">
      <Link to="/dang-nhap">Đăng nhập</Link> {children}.
    </p>
  )
}
