import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { admin } from '../../api/index.js'
import { ErrorBox, Loading } from '../../components/Ui.jsx'
import Icon from '../../components/Icon.jsx'
import { compactNumber, fullNumber } from '../../lib/format.js'
import { BarList, LineChart } from './Charts.jsx'

function Stat({ label, value, icon, to, accent }) {
  const content = (
    <>
      <span className={`stat-icon ${accent ? 'stat-icon-accent' : ''}`}>
        <Icon name={icon} size={20} />
      </span>
      <strong title={fullNumber(value)}>{compactNumber(value)}</strong>
      <span>{label}</span>
    </>
  )
  return to ? (
    <Link to={to} className="stat">
      {content}
    </Link>
  ) : (
    <div className="stat">{content}</div>
  )
}

export default function Dashboard() {
  const summary = useQuery({ queryKey: ['admin', 'summary'], queryFn: admin.summary })
  const views = useQuery({ queryKey: ['admin', 'views'], queryFn: () => admin.views() })
  const newUsers = useQuery({ queryKey: ['admin', 'new-users'], queryFn: () => admin.newUsers() })
  const topComics = useQuery({ queryKey: ['admin', 'top-comics'], queryFn: () => admin.topComics({ limit: 8 }) })
  const topGenres = useQuery({ queryKey: ['admin', 'top-genres'], queryFn: () => admin.topGenres({ limit: 8 }) })

  if (summary.isLoading) return <Loading />
  if (summary.isError) return <ErrorBox error={summary.error} onRetry={summary.refetch} />
  const data = summary.data

  return (
    <>
      <h1 className="admin-title">Tổng quan</h1>
      <div className="stat-grid">
        <Stat label="Lượt xem hôm nay" value={data.viewsToday} icon="eye" accent />
        <Stat label="Tổng lượt xem" value={data.totalViews} icon="fire" />
        <Stat label="Truyện" value={data.comics} icon="book" to="/admin/truyen" />
        <Stat label="Chương đã đăng" value={data.chapters} icon="list" />
        <Stat label="Người dùng" value={data.users} icon="users" to="/admin/nguoi-dung" />
        <Stat label="Thành viên mới hôm nay" value={data.newUsersToday} icon="user" />
        <Stat label="Báo cáo chờ xử lý" value={data.pendingReports} icon="flag" to="/admin/bao-cao" accent={data.pendingReports > 0} />
      </div>

      <section className="panel">
        <h2>Lượt xem 30 ngày qua</h2>
        {views.data ? <LineChart data={views.data} unit="lượt xem" /> : <Loading />}
      </section>

      <div className="admin-columns">
        <section className="panel">
          <h2>Truyện xem nhiều</h2>
          {topComics.data ? <BarList items={topComics.data.map((item) => ({ key: item.id, label: item.title, value: item.views }))} /> : <Loading />}
        </section>
        <section className="panel">
          <h2>Thể loại xem nhiều</h2>
          {topGenres.data ? <BarList items={topGenres.data.map((item) => ({ key: item.id, label: item.name, value: item.views }))} /> : <Loading />}
        </section>
      </div>

      <section className="panel">
        <h2>Thành viên mới 30 ngày qua</h2>
        {newUsers.data ? <LineChart data={newUsers.data} unit="thành viên" /> : <Loading />}
      </section>
    </>
  )
}
