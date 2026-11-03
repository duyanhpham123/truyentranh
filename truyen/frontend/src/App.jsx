import { lazy, Suspense, useEffect } from 'react'
import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth/AuthContext.jsx'
import Layout from './components/Layout.jsx'
import { Loading } from './components/Ui.jsx'
import Home from './pages/Home.jsx'
import Browse from './pages/Browse.jsx'
import ComicDetail from './pages/ComicDetail.jsx'
import Reader from './pages/Reader.jsx'
import { ForgotPassword, Login, Register, ResetPassword, VerifyEmail } from './pages/AuthPages.jsx'
import Profile from './pages/Profile.jsx'
import Library from './pages/Library.jsx'
import Notifications from './pages/Notifications.jsx'
import NotFound from './pages/NotFound.jsx'

// Khu quản trị tách riêng để độc giả không phải tải phần code này
const AdminLayout = lazy(() => import('./pages/admin/AdminLayout.jsx'))
const Dashboard = lazy(() => import('./pages/admin/Dashboard.jsx'))
const AdminComics = lazy(() => import('./pages/admin/AdminComics.jsx'))
const AdminChapters = lazy(() => import('./pages/admin/AdminChapters.jsx'))
const AdminGenres = lazy(() => import('./pages/admin/AdminGenres.jsx'))
const AdminAuthors = lazy(() => import('./pages/admin/AdminAuthors.jsx'))
const AdminUsers = lazy(() => import('./pages/admin/AdminUsers.jsx'))
const AdminReports = lazy(() => import('./pages/admin/AdminReports.jsx'))
const AdminAudit = lazy(() => import('./pages/admin/AdminAudit.jsx'))

function ScrollToTop() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

function RequireAuth({ admin = false }) {
  const { user, ready, isAdmin } = useAuth()
  const location = useLocation()
  if (!ready) return <Loading />
  if (!user) return <Navigate to="/dang-nhap" replace state={{ from: location.pathname }} />
  if (admin && !isAdmin) return <Navigate to="/" replace />
  return <Outlet />
}

export default function App() {
  return (
    <>
      <ScrollToTop />
      <Suspense fallback={<Loading />}>
        <Routes>
          <Route path="truyen/:slug/chuong/:number" element={<Reader />} />
          <Route element={<Layout />}>
            <Route index element={<Home />} />
            <Route path="truyen" element={<Browse />} />
            <Route path="truyen/:slug" element={<ComicDetail />} />
            <Route path="dang-nhap" element={<Login />} />
            <Route path="dang-ky" element={<Register />} />
            <Route path="quen-mat-khau" element={<ForgotPassword />} />
            <Route path="reset-password" element={<ResetPassword />} />
            <Route path="verify-email" element={<VerifyEmail />} />
            <Route path="tu-truyen" element={<Library />} />
            <Route element={<RequireAuth />}>
              <Route path="tai-khoan" element={<Profile />} />
              <Route path="thong-bao" element={<Notifications />} />
            </Route>
            <Route path="*" element={<NotFound />} />
          </Route>
          <Route element={<RequireAuth admin />}>
            <Route path="admin" element={<AdminLayout />}>
              <Route index element={<Dashboard />} />
              <Route path="truyen" element={<AdminComics />} />
              <Route path="truyen/:comicId/chuong" element={<AdminChapters />} />
              <Route path="the-loai" element={<AdminGenres />} />
              <Route path="tac-gia" element={<AdminAuthors />} />
              <Route path="nguoi-dung" element={<AdminUsers />} />
              <Route path="bao-cao" element={<AdminReports />} />
              <Route path="nhat-ky" element={<AdminAudit />} />
            </Route>
          </Route>
        </Routes>
      </Suspense>
    </>
  )
}
