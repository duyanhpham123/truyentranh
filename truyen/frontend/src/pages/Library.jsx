import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { me } from '../api/index.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { Cover } from '../components/ComicArt.jsx'
import { Empty, Loading, LoginPrompt, Pagination } from '../components/Ui.jsx'
import Icon from '../components/Icon.jsx'
import { guestHistory, removeGuestProgress } from '../lib/guestHistory.js'
import { chapterLabel, timeAgo } from '../lib/format.js'

function LibraryRow({ to, title, coverUrl, line, time, onRemove }) {
  return (
    <li className="library-row">
      <Link to={to}>
        <Cover url={coverUrl} title={title} />
        <span>
          <strong>{title}</strong>
          <small>{line}</small>
          <small className="muted">{time}</small>
        </span>
      </Link>
      {onRemove && (
        <button className="icon-btn" onClick={onRemove} aria-label={`Xóa ${title} khỏi lịch sử`}>
          <Icon name="trash" size={18} />
        </button>
      )}
    </li>
  )
}

function ServerHistory() {
  const [page, setPage] = useState(0)
  const queryClient = useQueryClient()
  const { data, isLoading } = useQuery({ queryKey: ['history', page], queryFn: () => me.history({ page, size: 12 }) })
  const remove = useMutation({
    mutationFn: me.deleteHistory,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['history'] }),
  })
  if (isLoading) return <Loading />
  if (!data.items.length) return <EmptyHistory />
  return (
    <>
      <ul className="library-grid">
        {data.items.map((item) => (
          <LibraryRow
            key={item.comicId}
            to={`/truyen/${item.comicSlug}/chuong/${chapterLabel(item.chapterNumber)}`}
            title={item.comicTitle}
            coverUrl={item.coverUrl}
            line={`Đọc tiếp chương ${chapterLabel(item.chapterNumber)}, trang ${item.lastPage}`}
            time={timeAgo(item.lastReadAt)}
            onRemove={() => remove.mutate(item.comicId)}
          />
        ))}
      </ul>
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </>
  )
}

function GuestHistory() {
  const [items, setItems] = useState(guestHistory)
  if (!items.length) return <EmptyHistory />
  return (
    <>
      <LoginPrompt>để lưu lịch sử đọc trên mọi thiết bị</LoginPrompt>
      <ul className="library-grid">
        {items.map((item) => (
          <LibraryRow
            key={item.comicId}
            to={`/truyen/${item.comicSlug}/chuong/${chapterLabel(item.chapterNumber)}`}
            title={item.comicTitle}
            coverUrl={item.coverUrl}
            line={`Đọc tiếp chương ${chapterLabel(item.chapterNumber)}, trang ${item.lastPage}`}
            time={timeAgo(item.lastReadAt)}
            onRemove={() => {
              removeGuestProgress(item.comicId)
              setItems(guestHistory())
            }}
          />
        ))}
      </ul>
    </>
  )
}

function EmptyHistory() {
  return (
    <Empty title="Bạn chưa đọc truyện nào" action={<Link className="btn btn-primary" to="/truyen">Tìm truyện để đọc</Link>}>
      Truyện bạn đọc sẽ hiện ở đây để đọc tiếp đúng trang đang dở.
    </Empty>
  )
}

function Follows() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useQuery({ queryKey: ['follows', page], queryFn: () => me.follows({ page, size: 12 }) })
  if (isLoading) return <Loading />
  if (!data.items.length) {
    return (
      <Empty title="Bạn chưa theo dõi truyện nào" action={<Link className="btn btn-primary" to="/truyen">Tìm truyện hay</Link>}>
        Theo dõi để nhận thông báo khi có chương mới.
      </Empty>
    )
  }
  return (
    <>
      <ul className="library-grid">
        {data.items.map((item) => (
          <LibraryRow
            key={item.comicId}
            to={`/truyen/${item.comicSlug}`}
            title={item.comicTitle}
            coverUrl={item.coverUrl}
            line={item.lastChapterAt ? `Chương mới ${timeAgo(item.lastChapterAt)}` : 'Chưa có chương'}
            time={`Theo dõi ${timeAgo(item.followedAt)}`}
          />
        ))}
      </ul>
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </>
  )
}

export default function Library() {
  const { user, ready } = useAuth()
  const [tab, setTab] = useState('history')
  if (!ready) return <Loading />
  return (
    <>
      <div className="page-banner">
        <div className="container">
          <h1>Tủ truyện</h1>
          <p>Những truyện bạn đang đọc và theo dõi.</p>
        </div>
      </div>
      <div className="container section">
        <div className="tabs tabs-spaced" role="tablist">
          <button role="tab" aria-selected={tab === 'history'} className={tab === 'history' ? 'is-active' : ''} onClick={() => setTab('history')}>
            Lịch sử đọc
          </button>
          <button role="tab" aria-selected={tab === 'follows'} className={tab === 'follows' ? 'is-active' : ''} onClick={() => setTab('follows')}>
            Đang theo dõi
          </button>
        </div>
        {tab === 'history' ? (
          user ? <ServerHistory /> : <GuestHistory />
        ) : user ? (
          <Follows />
        ) : (
          <Empty title="Đăng nhập để theo dõi truyện" action={<Link className="btn btn-primary" to="/dang-nhap">Đăng nhập</Link>}>
            Bạn sẽ nhận thông báo mỗi khi truyện yêu thích có chương mới.
          </Empty>
        )}
      </div>
    </>
  )
}
