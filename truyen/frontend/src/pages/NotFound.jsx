import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="not-found">
      <span className="not-found-code">404</span>
      <h1>Ối! Trang này bị xé mất rồi.</h1>
      <p>Đường dẫn không tồn tại hoặc đã bị gỡ.</p>
      <Link className="btn btn-primary btn-large" to="/">
        Về trang chủ
      </Link>
    </div>
  )
}
