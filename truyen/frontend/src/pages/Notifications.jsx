import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { me } from '../api/index.js'
import { Empty, Loading, Pagination } from '../components/Ui.jsx'
import Icon from '../components/Icon.jsx'
import { chapterLabel, timeAgo } from '../lib/format.js'

export default function Notifications() {
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data, isLoading } = useQuery({ queryKey: ['notifications', page], queryFn: () => me.notifications({ page, size: 15 }) })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['notifications'] })
    queryClient.invalidateQueries({ queryKey: ['unread-count'] })
  }
  const markRead = useMutation({ mutationFn: me.markRead, onSuccess: refresh })
  const markAll = useMutation({ mutationFn: me.markAllRead, onSuccess: refresh })

  if (isLoading) return <Loading />
  const hasUnread = data.items.some((item) => !item.readAt)

  return (
    <>
      <div className="page-banner">
        <div className="container">
          <h1>Thông báo</h1>
          <p>Chương mới từ những truyện bạn theo dõi.</p>
        </div>
      </div>
      <div className="container section narrow">
        {hasUnread && (
          <button className="btn btn-small notif-all" disabled={markAll.isPending} onClick={() => markAll.mutate()}>
            Đánh dấu đã đọc tất cả
          </button>
        )}
        {data.items.length ? (
          <>
            <ul className="notif-list">
              {data.items.map((item) => (
                <li key={item.id}>
                  <button
                    className={`notif ${item.readAt ? '' : 'is-unread'}`}
                    onClick={() => {
                      if (!item.readAt) markRead.mutate(item.id)
                      navigate(`/truyen/${item.comicSlug}/chuong/${chapterLabel(item.chapterNumber)}`)
                    }}
                  >
                    <span className="notif-icon">
                      <Icon name="bell" size={18} />
                    </span>
                    <span className="notif-text">
                      <strong>{item.message}</strong>
                      <small>{timeAgo(item.createdAt)}</small>
                    </span>
                    {!item.readAt && <span className="notif-dot" aria-label="Chưa đọc" />}
                  </button>
                </li>
              ))}
            </ul>
            <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
          </>
        ) : (
          <Empty title="Chưa có thông báo nào" action={<Link className="btn" to="/truyen">Tìm truyện để theo dõi</Link>} />
        )}
      </div>
    </>
  )
}
