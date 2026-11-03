import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { comics } from '../api/index.js'
import { errorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { Cover } from '../components/ComicArt.jsx'
import Comments from '../components/Comments.jsx'
import { useToast } from '../components/Toast.jsx'
import { Empty, ErrorBox, Loading, Stars } from '../components/Ui.jsx'
import Icon from '../components/Icon.jsx'
import { guestLastRead } from '../lib/guestHistory.js'
import { chapterLabel, chapterTitleText, compactNumber, formatDate, STATUS_LABEL, timeAgo } from '../lib/format.js'

function ChapterList({ slug, chapters, lastReadNumber }) {
  const [filter, setFilter] = useState('')
  const [ascending, setAscending] = useState(false)
  const visible = useMemo(() => {
    const list = chapters.filter((chapter) => {
      const text = `${chapterLabel(chapter.chapterNumber)} ${chapter.chapterTitle ?? ''}`.toLowerCase()
      return text.includes(filter.trim().toLowerCase())
    })
    return ascending ? [...list].reverse() : list
  }, [chapters, filter, ascending])

  return (
    <section className="panel chapter-panel">
      <div className="chapter-panel-head">
        <h2>
          Danh sách chương <span className="count-pill">{chapters.length}</span>
        </h2>
        <div className="chapter-panel-tools">
          <input type="search" value={filter} onChange={(event) => setFilter(event.target.value)} placeholder="Tìm chương..." aria-label="Tìm chương" />
          <button className="btn btn-small" onClick={() => setAscending((value) => !value)}>
            <Icon name={ascending ? 'up' : 'down'} size={16} /> {ascending ? 'Cũ nhất' : 'Mới nhất'}
          </button>
        </div>
      </div>
      {chapters.length === 0 ? (
        <Empty title="Chưa có chương nào">Truyện sắp ra mắt, hãy theo dõi để nhận thông báo nhé.</Empty>
      ) : (
        <ul className="chapter-list">
          {visible.map((chapter) => {
            const label = chapterLabel(chapter.chapterNumber)
            return (
              <li key={chapter.id}>
                <Link to={`/truyen/${slug}/chuong/${label}`} className={label === lastReadNumber ? 'is-current' : ''}>
                  <strong>Chương {label}</strong>
                  <span className="chapter-title">{chapterTitleText(chapter)}</span>
                  {label === lastReadNumber && <span className="sticker sticker-inline">Đang đọc</span>}
                  <small>
                    <Icon name="eye" size={13} /> {compactNumber(chapter.views)}
                  </small>
                  <time dateTime={chapter.publishedAt} title={formatDate(chapter.publishedAt)}>
                    {timeAgo(chapter.publishedAt)}
                  </time>
                </Link>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}

export default function ComicDetail() {
  const { slug } = useParams()
  const { user, ready } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const detail = useQuery({ queryKey: ['comic', slug], queryFn: () => comics.detail(slug) })
  const chapters = useQuery({ queryKey: ['chapters', slug], queryFn: () => comics.chapters(slug) })
  const comic = detail.data
  const mine = useQuery({
    queryKey: ['comic-me', comic?.id],
    queryFn: () => comics.myState(comic.id),
    enabled: ready && Boolean(user && comic),
  })

  useEffect(() => {
    if (comic) document.title = `${comic.title} – Truyện BÙM!`
    return () => {
      document.title = 'Truyện BÙM! – Đọc truyện tranh online'
    }
  }, [comic])

  const requireLogin = () => {
    if (user) return true
    navigate('/dang-nhap', { state: { from: `/truyen/${slug}` } })
    return false
  }

  const onError = (error) => toast.error(errorMessage(error))
  const follow = useMutation({
    mutationFn: () => (mine.data?.following ? comics.unfollow(comic.id) : comics.follow(comic.id)),
    onSuccess: () => {
      toast.success(mine.data?.following ? 'Đã bỏ theo dõi.' : 'Đã theo dõi! Bạn sẽ nhận thông báo khi có chương mới.')
      queryClient.invalidateQueries({ queryKey: ['comic-me', comic.id] })
    },
    onError,
  })
  const rate = useMutation({
    mutationFn: (score) => comics.rate(comic.id, score),
    onSuccess: () => {
      toast.success('Cảm ơn bạn đã chấm điểm!')
      queryClient.invalidateQueries({ queryKey: ['comic-me', comic.id] })
      queryClient.invalidateQueries({ queryKey: ['comic', slug] })
    },
    onError,
  })

  if (detail.isLoading) return <Loading />
  if (detail.isError) {
    if (detail.error?.response?.status === 404) {
      return (
        <div className="container section">
          <Empty title="Không tìm thấy truyện này" action={<Link className="btn" to="/truyen">Xem truyện khác</Link>}>
            Truyện có thể đã bị ẩn hoặc gỡ.
          </Empty>
        </div>
      )
    }
    return (
      <div className="container section">
        <ErrorBox error={detail.error} onRetry={detail.refetch} />
      </div>
    )
  }

  const chapterItems = chapters.data ?? []
  const firstChapter = chapterItems[chapterItems.length - 1]
  const serverLast = mine.data?.lastRead
  const guestLast = user ? null : guestLastRead(comic.id)
  const lastReadNumber = serverLast ? chapterLabel(serverLast.chapterNumber) : guestLast ? chapterLabel(guestLast.chapterNumber) : null

  return (
    <>
      <section className="detail-hero">
        <div className="detail-hero-bg" aria-hidden="true" />
        <div className="container detail-hero-inner">
          <div className="detail-cover">
            <Cover url={comic.coverUrl} title={comic.title} />
          </div>
          <div className="detail-info">
            <span className="sticker sticker-inline">{STATUS_LABEL[comic.status]}</span>
            <h1>{comic.title}</h1>
            {comic.authorName && (
              <p className="detail-author">
                Tác giả: <Link to={`/truyen?authorId=${comic.authorId}`}>{comic.authorName}</Link>
              </p>
            )}
            <div className="chip-row">
              {comic.genres.map((genre) => (
                <Link key={genre.id} to={`/truyen?genre=${genre.slug}`} className="chip">
                  {genre.name}
                </Link>
              ))}
            </div>
            <div className="detail-stats">
              <div>
                <strong>{compactNumber(comic.views)}</strong>
                <span>Lượt xem</span>
              </div>
              <div>
                <strong>{comic.ratingCount ? Number(comic.ratingAvg).toFixed(1) : '–'}</strong>
                <span>{comic.ratingCount} đánh giá</span>
              </div>
              <div>
                <strong>{chapterItems.length}</strong>
                <span>Chương</span>
              </div>
            </div>
            <div className="detail-actions">
              {lastReadNumber ? (
                <Link className="btn btn-primary btn-large" to={`/truyen/${slug}/chuong/${lastReadNumber}`}>
                  Đọc tiếp chương {lastReadNumber} <Icon name="right" size={18} />
                </Link>
              ) : firstChapter ? (
                <Link className="btn btn-primary btn-large" to={`/truyen/${slug}/chuong/${chapterLabel(firstChapter.chapterNumber)}`}>
                  Đọc từ đầu <Icon name="right" size={18} />
                </Link>
              ) : null}
              <button
                className={`btn btn-large ${mine.data?.following ? 'btn-on' : ''}`}
                aria-pressed={Boolean(mine.data?.following)}
                disabled={follow.isPending}
                onClick={() => requireLogin() && follow.mutate()}
              >
                <Icon name="heart" size={18} filled={Boolean(mine.data?.following)} />
                {mine.data?.following ? 'Đang theo dõi' : 'Theo dõi'}
              </button>
            </div>
            <div className="detail-rate">
              <span>{mine.data?.myRating ? 'Điểm của bạn:' : 'Chấm điểm:'}</span>
              <Stars value={mine.data?.myRating ?? 0} size={24} onRate={(score) => requireLogin() && rate.mutate(score)} />
            </div>
          </div>
        </div>
      </section>

      <div className="container detail-body">
        <div className="detail-main">
          {comic.description && (
            <section className="panel speech-panel">
              <h2>Nội dung</h2>
              <p>{comic.description}</p>
            </section>
          )}
          {chapters.isLoading ? <Loading /> : <ChapterList slug={slug} chapters={chapterItems} lastReadNumber={lastReadNumber} />}
          <div className="panel">
            <Comments comicId={comic.id} />
          </div>
        </div>
      </div>
    </>
  )
}
