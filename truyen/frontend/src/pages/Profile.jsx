import { useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { auth, me } from '../api/index.js'
import { errorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { useToast } from '../components/Toast.jsx'
import { Avatar, Field } from '../components/Ui.jsx'
import Icon from '../components/Icon.jsx'
import { formatDate } from '../lib/format.js'

export default function Profile() {
  const { user, setUser } = useAuth()
  const toast = useToast()
  const fileRef = useRef(null)
  const [fullName, setFullName] = useState(user.fullName ?? '')
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '' })
  const onError = (error) => toast.error(errorMessage(error))

  const saveProfile = useMutation({
    mutationFn: () => me.update(fullName),
    onSuccess: (updated) => {
      setUser(updated)
      toast.success('Đã lưu hồ sơ.')
    },
    onError,
  })
  const saveAvatar = useMutation({
    mutationFn: (file) => me.avatar(file),
    onSuccess: (updated) => {
      setUser(updated)
      toast.success('Đã đổi ảnh đại diện.')
    },
    onError,
  })
  const savePassword = useMutation({
    mutationFn: () => me.password(passwords),
    onSuccess: () => {
      setPasswords({ currentPassword: '', newPassword: '' })
      toast.success('Đã đổi mật khẩu.')
    },
    onError,
  })
  const resend = useMutation({
    mutationFn: () => auth.resend(user.email),
    onSuccess: () => toast.success('Đã gửi lại email xác thực.'),
    onError,
  })

  return (
    <div className="container section profile">
      <div className="profile-card panel">
        <div className="profile-avatar">
          <Avatar url={user.avatarUrl} name={user.fullName} size={120} />
          <button className="btn btn-small" disabled={saveAvatar.isPending} onClick={() => fileRef.current.click()}>
            <Icon name="upload" size={16} /> {saveAvatar.isPending ? 'Đang tải...' : 'Đổi ảnh'}
          </button>
          <input
            ref={fileRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            hidden
            aria-label="Chọn ảnh đại diện"
            onChange={(event) => {
              const file = event.target.files[0]
              if (file) saveAvatar.mutate(file)
              event.target.value = ''
            }}
          />
        </div>
        <div>
          <h1>{user.fullName}</h1>
          <p className="muted">
            @{user.username} · {user.email}
          </p>
          <p className="muted">Tham gia từ {formatDate(user.createdAt)}</p>
          <div className="chip-row">
            {user.roles.map((role) => (
              <span key={role} className="chip chip-plain">
                {role}
              </span>
            ))}
            <span className={`chip chip-plain ${user.emailVerified ? 'chip-good' : 'chip-warn'}`}>{user.emailVerified ? 'Email đã xác thực' : 'Email chưa xác thực'}</span>
          </div>
          {!user.emailVerified && (
            <button className="btn btn-small" disabled={resend.isPending} onClick={() => resend.mutate()}>
              Gửi lại email xác thực
            </button>
          )}
        </div>
      </div>

      <div className="profile-forms">
        <form
          className="panel form"
          onSubmit={(event) => {
            event.preventDefault()
            saveProfile.mutate()
          }}
        >
          <h2>Hồ sơ</h2>
          <Field label="Tên hiển thị">
            <input value={fullName} onChange={(event) => setFullName(event.target.value)} maxLength={100} required />
          </Field>
          <button className="btn btn-primary" disabled={saveProfile.isPending}>
            Lưu thay đổi
          </button>
        </form>
        <form
          className="panel form"
          onSubmit={(event) => {
            event.preventDefault()
            savePassword.mutate()
          }}
        >
          <h2>Đổi mật khẩu</h2>
          <Field label="Mật khẩu hiện tại">
            <input type="password" value={passwords.currentPassword} onChange={(event) => setPasswords({ ...passwords, currentPassword: event.target.value })} autoComplete="current-password" required />
          </Field>
          <Field label="Mật khẩu mới" hint="Ít nhất 8 ký tự">
            <input type="password" value={passwords.newPassword} onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })} minLength={8} maxLength={72} autoComplete="new-password" required />
          </Field>
          <button className="btn btn-primary" disabled={savePassword.isPending}>
            Đổi mật khẩu
          </button>
        </form>
      </div>
    </div>
  )
}
