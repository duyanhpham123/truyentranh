import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'
import { Logo } from '../../components/Layout.jsx'
import { Avatar } from '../../components/Ui.jsx'
import Icon from '../../components/Icon.jsx'
import { useTheme } from '../../lib/useTheme.js'

const ITEMS = [
  { to: '/admin', icon: 'chart', label: 'Tổng quan', end: true },
  { to: '/admin/truyen', icon: 'book', label: 'Truyện' },
  { to: '/admin/the-loai', icon: 'tag', label: 'Thể loại' },
  { to: '/admin/tac-gia', icon: 'pen', label: 'Tác giả' },
  { to: '/admin/nguoi-dung', icon: 'users', label: 'Người dùng' },
  { to: '/admin/bao-cao', icon: 'flag', label: 'Báo cáo' },
  { to: '/admin/nhat-ky', icon: 'log', label: 'Nhật ký' },
]

export default function AdminLayout() {
  const { user } = useAuth()
  const [theme, toggleTheme] = useTheme()
  return (
    <div className="admin">
      <aside className="admin-side">
        <Logo />
        <nav aria-label="Quản trị">
          {ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className="admin-link">
              <Icon name={item.icon} size={18} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <Link to="/" className="admin-link admin-back">
          <Icon name="left" size={18} />
          <span>Về trang đọc</span>
        </Link>
      </aside>
      <div className="admin-main">
        <div className="admin-top">
          <span className="admin-badge">Khu quản trị</span>
          <div className="header-actions">
            <button className="icon-btn" onClick={toggleTheme} aria-label={theme === 'dark' ? 'Chuyển sang nền sáng' : 'Chuyển sang nền tối'}>
              <Icon name={theme === 'dark' ? 'sun' : 'moon'} />
            </button>
            <Avatar url={user.avatarUrl} name={user.fullName} size={38} />
          </div>
        </div>
        <div className="admin-content">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
