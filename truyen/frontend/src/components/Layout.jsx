import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthContext.jsx'
import { catalog, me } from '../api/index.js'
import { useTheme } from '../lib/useTheme.js'
import { Avatar } from './Ui.jsx'
import Icon from './Icon.jsx'

export function Logo() {
  return (
    <Link to="/" className="logo" aria-label="Truyện BÙM! - Trang chủ">
      <span className="logo-text">Truyện</span>
      <span className="logo-burst">BÙM!</span>
    </Link>
  )
}

function SearchBox() {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  return (
    <form
      className="search"
      role="search"
      onSubmit={(event) => {
        event.preventDefault()
        navigate(keyword.trim() ? `/truyen?keyword=${encodeURIComponent(keyword.trim())}` : '/truyen')
      }}
    >
      <input type="search" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm truyện, tác giả..." aria-label="Tìm truyện" />
      <button aria-label="Tìm kiếm">
        <Icon name="search" size={18} />
      </button>
    </form>
  )
}

function GenreMenu() {
  const { data: genres = [] } = useQuery({ queryKey: ['genres'], queryFn: catalog.genres, staleTime: 300_000 })
  return (
    <div className="nav-dropdown">
      <button className="nav-link" aria-haspopup="true">
        Thể loại <Icon name="down" size={14} />
      </button>
      <div className="nav-dropdown-panel">
        {genres.map((genre) => (
          <Link key={genre.id} to={`/truyen?genre=${genre.slug}`}>
            {genre.name}
          </Link>
        ))}
      </div>
    </div>
  )
}

function NotificationBell() {
  const { data } = useQuery({ queryKey: ['unread-count'], queryFn: me.unreadCount, refetchInterval: 60_000 })
  const count = data?.count ?? 0
  return (
    <Link to="/thong-bao" className="icon-btn" aria-label={count ? `Thông báo, ${count} chưa đọc` : 'Thông báo'}>
      <Icon name="bell" />
      {count > 0 && <span className="badge">{count > 99 ? '99+' : count}</span>}
    </Link>
  )
}

function UserMenu() {
  const { user, logout, isAdmin } = useAuth()
  const [open, setOpen] = useState(false)
  const ref = useRef(null)
  const navigate = useNavigate()
  const { pathname } = useLocation()

  useEffect(() => setOpen(false), [pathname])
  useEffect(() => {
    if (!open) return undefined
    const close = (event) => {
      if (!ref.current?.contains(event.target)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])

  return (
    <div className="user-menu" ref={ref}>
      <button className="user-menu-btn" onClick={() => setOpen((value) => !value)} aria-expanded={open} aria-label="Tài khoản">
        <Avatar url={user.avatarUrl} name={user.fullName} size={38} />
      </button>
      {open && (
        <div className="user-menu-panel">
          <div className="user-menu-name">
            <strong>{user.fullName}</strong>
            <span>@{user.username}</span>
          </div>
          <Link to="/tai-khoan">
            <Icon name="user" size={18} /> Tài khoản
          </Link>
          <Link to="/tu-truyen">
            <Icon name="book" size={18} /> Tủ truyện
          </Link>
          {isAdmin && (
            <Link to="/admin">
              <Icon name="chart" size={18} /> Quản trị
            </Link>
          )}
          <button
            onClick={async () => {
              await logout()
              navigate('/')
            }}
          >
            <Icon name="logout" size={18} /> Đăng xuất
          </button>
        </div>
      )}
    </div>
  )
}

function Header() {
  const { user, ready } = useAuth()
  const [theme, toggleTheme] = useTheme()
  return (
    <header className="header">
      <div className="container header-inner">
        <Logo />
        <nav className="nav" aria-label="Điều hướng chính">
          <NavLink to="/truyen" className="nav-link" end>
            Tất cả truyện
          </NavLink>
          <GenreMenu />
          <NavLink to="/truyen?sort=rating" className="nav-link">
            Đánh giá cao
          </NavLink>
          <NavLink to="/tu-truyen" className="nav-link">
            Tủ truyện
          </NavLink>
        </nav>
        <SearchBox />
        <div className="header-actions">
          <button className="icon-btn" onClick={toggleTheme} aria-label={theme === 'dark' ? 'Chuyển sang nền sáng' : 'Chuyển sang nền tối'}>
            <Icon name={theme === 'dark' ? 'sun' : 'moon'} />
          </button>
          {ready &&
            (user ? (
              <>
                <NotificationBell />
                <UserMenu />
              </>
            ) : (
              <Link to="/dang-nhap" className="btn btn-primary btn-small">
                Đăng nhập
              </Link>
            ))}
        </div>
      </div>
    </header>
  )
}

function BottomNav() {
  const { user } = useAuth()
  const items = [
    { to: '/', icon: 'home', label: 'Trang chủ', end: true },
    { to: '/truyen', icon: 'grid', label: 'Duyệt' },
    { to: '/tu-truyen', icon: 'book', label: 'Tủ truyện' },
    { to: user ? '/tai-khoan' : '/dang-nhap', icon: 'user', label: user ? 'Tôi' : 'Đăng nhập' },
  ]
  return (
    <nav className="bottom-nav" aria-label="Điều hướng nhanh">
      {items.map((item) => (
        <NavLink key={item.to} to={item.to} end={item.end} className="bottom-nav-item">
          <Icon name={item.icon} />
          <span>{item.label}</span>
        </NavLink>
      ))}
    </nav>
  )
}

function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <Logo />
        <p>Đọc truyện tranh online, cập nhật chương mới mỗi ngày. Dữ liệu hiện tại là dữ liệu mẫu.</p>
        <nav aria-label="Liên kết cuối trang">
          <Link to="/truyen">Tất cả truyện</Link>
          <Link to="/tu-truyen">Tủ truyện</Link>
          <Link to="/dang-ky">Đăng ký</Link>
        </nav>
      </div>
    </footer>
  )
}

export default function Layout() {
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">
        Tới nội dung chính
      </a>
      <Header />
      <main id="main" className="main">
        <Outlet />
      </main>
      <Footer />
      <BottomNav />
    </div>
  )
}
