import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { auth } from '../api/index.js'
import { errorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { useToast } from '../components/Toast.jsx'
import { Field, Loading } from '../components/Ui.jsx'

function AuthShell({ title, burst, children, footer }) {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <span className="burst-label">{burst}</span>
        <h1>{title}</h1>
        {children}
        {footer && <div className="auth-footer">{footer}</div>}
      </div>
    </div>
  )
}

function useSubmit(action) {
  const [error, setError] = useState(null)
  const [pending, setPending] = useState(false)
  const submit = async (event) => {
    event.preventDefault()
    setError(null)
    setPending(true)
    try {
      await action()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setPending(false)
    }
  }
  return { error, pending, submit }
}

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const { error, pending, submit } = useSubmit(async () => {
    await login({ email, password })
    navigate(location.state?.from ?? '/', { replace: true })
  })
  return (
    <AuthShell
      title="Đăng nhập"
      burst="Chào lại nha!"
      footer={
        <>
          Chưa có tài khoản? <Link to="/dang-ky">Đăng ký ngay</Link>
        </>
      }
    >
      <form onSubmit={submit} className="form">
        <Field label="Email">
          <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required />
        </Field>
        <Field label="Mật khẩu">
          <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required />
        </Field>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-large btn-block" disabled={pending}>
          {pending ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
        <Link to="/quen-mat-khau" className="form-link">
          Quên mật khẩu?
        </Link>
      </form>
    </AuthShell>
  )
}

export function Register() {
  const navigate = useNavigate()
  const toast = useToast()
  const { login } = useAuth()
  const [form, setForm] = useState({ username: '', email: '', fullName: '', password: '' })
  const set = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))
  const { error, pending, submit } = useSubmit(async () => {
    await auth.register(form)
    await login({ email: form.email, password: form.password })
    toast.success('Đăng ký thành công! Hãy kiểm tra email để xác thực tài khoản.')
    navigate('/')
  })
  return (
    <AuthShell
      title="Tạo tài khoản"
      burst="Gia nhập nào!"
      footer={
        <>
          Đã có tài khoản? <Link to="/dang-nhap">Đăng nhập</Link>
        </>
      }
    >
      <form onSubmit={submit} className="form">
        <Field label="Tên hiển thị">
          <input value={form.fullName} onChange={set('fullName')} maxLength={100} autoComplete="name" required />
        </Field>
        <Field label="Tên đăng nhập" hint="Chữ không dấu, số, dấu chấm hoặc gạch dưới, từ 3 ký tự">
          <input value={form.username} onChange={set('username')} minLength={3} maxLength={50} pattern="[a-zA-Z0-9_.]+" autoComplete="username" required />
        </Field>
        <Field label="Email">
          <input type="email" value={form.email} onChange={set('email')} maxLength={100} autoComplete="email" required />
        </Field>
        <Field label="Mật khẩu" hint="Ít nhất 8 ký tự">
          <input type="password" value={form.password} onChange={set('password')} minLength={8} maxLength={72} autoComplete="new-password" required />
        </Field>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-large btn-block" disabled={pending}>
          {pending ? 'Đang tạo...' : 'Đăng ký'}
        </button>
      </form>
    </AuthShell>
  )
}

export function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const { error, pending, submit } = useSubmit(async () => {
    await auth.forgot(email)
    setSent(true)
  })
  return (
    <AuthShell title="Quên mật khẩu" burst="Đừng lo!" footer={<Link to="/dang-nhap">Quay lại đăng nhập</Link>}>
      {sent ? (
        <p className="form-success">Nếu email này đã đăng ký, bạn sẽ nhận được liên kết đặt lại mật khẩu trong ít phút.</p>
      ) : (
        <form onSubmit={submit} className="form">
          <Field label="Email đã đăng ký">
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required />
          </Field>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="btn btn-primary btn-large btn-block" disabled={pending}>
            {pending ? 'Đang gửi...' : 'Gửi liên kết'}
          </button>
        </form>
      )}
    </AuthShell>
  )
}

export function ResetPassword() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const toast = useToast()
  const [newPassword, setNewPassword] = useState('')
  const { error, pending, submit } = useSubmit(async () => {
    await auth.reset({ token: params.get('token'), newPassword })
    toast.success('Đã đổi mật khẩu, hãy đăng nhập lại.')
    navigate('/dang-nhap')
  })
  return (
    <AuthShell title="Đặt lại mật khẩu" burst="Sắp xong rồi!">
      <form onSubmit={submit} className="form">
        <Field label="Mật khẩu mới" hint="Ít nhất 8 ký tự">
          <input type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength={8} maxLength={72} autoComplete="new-password" required />
        </Field>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="btn btn-primary btn-large btn-block" disabled={pending || !params.get('token')}>
          {pending ? 'Đang lưu...' : 'Lưu mật khẩu'}
        </button>
      </form>
    </AuthShell>
  )
}

export function VerifyEmail() {
  const [params] = useSearchParams()
  const { user, setUser } = useAuth()
  const [state, setState] = useState({ status: 'pending' })
  const started = useRef(false)

  useEffect(() => {
    // Token chỉ dùng được một lần, StrictMode gọi effect hai lần nên phải chặn
    if (started.current) return
    started.current = true
    auth
      .verify(params.get('token') ?? '')
      .then(() => setState({ status: 'done' }))
      .catch((error) => setState({ status: 'error', message: errorMessage(error) }))
  }, [params])

  useEffect(() => {
    if (state.status === 'done' && user && !user.emailVerified) setUser({ ...user, emailVerified: true })
  }, [state.status, user, setUser])

  return (
    <AuthShell title="Xác thực email" burst={state.status === 'done' ? 'Tuyệt!' : 'Chờ chút!'} footer={<Link to="/">Về trang chủ</Link>}>
      {state.status === 'pending' && <Loading label="Đang xác thực..." />}
      {state.status === 'done' && <p className="form-success">Email đã được xác thực. Giờ bạn có thể bình luận rồi!</p>}
      {state.status === 'error' && <p className="form-error">{state.message}</p>}
    </AuthShell>
  )
}
