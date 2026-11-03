import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { comics, me } from '../api/index.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { ChapterImage } from '../components/ComicArt.jsx'
import Comments from '../components/Comments.jsx'
import { Empty, ErrorBox, Loading } from '../components/Ui.jsx'
import Icon from '../components/Icon.jsx'
import { saveGuestProgress } from '../lib/guestHistory.js'
import { chapterLabel, chapterTitleText } from '../lib/format.js'

const SAVE_DELAY = 2500

export default function Reader() {
  const { slug, number } = useParams()
  const navigate = useNavigate()
  const { user, ready } = useAuth()
  const [currentPage, setCurrentPage] = useState(1)
  const [barHidden, setBarHidden] = useState(false)
  const pagesRef = useRef(null)

  const { data: chapter, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['chapter', slug, number],
    queryFn: () => comics.chapter(slug, number),
  })
  const { data: chapters = [] } = useQuery({ queryKey: ['chapters', slug], queryFn: () => comics.chapters(slug) })
  const { data: comic } = useQuery({ queryKey: ['comic', slug], queryFn: () => comics.detail(slug) })

  const goTo = (value) => value != null && navigate(`/truyen/${slug}/chuong/${chapterLabel(value)}`)

  useEffect(() => {
    window.scrollTo(0, 0)
    setCurrentPage(1)
  }, [slug, number])

  useEffect(() => {
    if (chapter) document.title = `${chapter.comicTitle} – Chương ${chapterLabel(chapter.chapterNumber)}`
    return () => {
      document.title = 'Truyện BÙM! – Đọc truyện tranh online'
    }
  }, [chapter])

  // Backend tự chống đếm trùng theo người đọc
  useEffect(() => {
    if (chapter) comics.recordView(chapter.id).catch(() => {})
  }, [chapter?.id])

  // Theo dõi trang đang hiện trên màn hình
  useEffect(() => {
    if (!chapter || !pagesRef.current) return undefined
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) setCurrentPage(Number(entry.target.dataset.page))
        }
      },
      { rootMargin: '-45% 0px -45% 0px' },
    )
    pagesRef.current.querySelectorAll('[data-page]').forEach((element) => observer.observe(element))
    return () => observer.disconnect()
  }, [chapter])

  // Lưu tiến độ sau khi người đọc dừng ở một trang vài giây
  useEffect(() => {
    if (!chapter || !ready) return undefined
    const timer = setTimeout(() => {
      if (user) {
        me.saveProgress(chapter.comicId, chapter.id, currentPage).catch(() => {})
      } else {
        saveGuestProgress({
          comicId: chapter.comicId,
          comicSlug: chapter.comicSlug,
          comicTitle: chapter.comicTitle,
          coverUrl: comic?.coverUrl ?? null,
          chapterId: chapter.id,
          chapterNumber: chapter.chapterNumber,
          lastPage: currentPage,
        })
      }
    }, SAVE_DELAY)
    return () => clearTimeout(timer)
  }, [chapter, currentPage, user, ready, comic?.coverUrl])

  // Mũi tên trái/phải để chuyển chương, ẩn thanh công cụ khi cuộn xuống
  useEffect(() => {
    const onKey = (event) => {
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes(event.target.tagName)) return
      if (event.key === 'ArrowLeft') goTo(chapter?.prevChapter)
      if (event.key === 'ArrowRight') goTo(chapter?.nextChapter)
    }
    let lastY = window.scrollY
    const onScroll = () => {
      const y = window.scrollY
      if (Math.abs(y - lastY) > 12) {
        setBarHidden(y > lastY && y > 200)
        lastY = y
      }
    }
    window.addEventListener('keydown', onKey)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => {
      window.removeEventListener('keydown', onKey)
      window.removeEventListener('scroll', onScroll)
    }
  }, [chapter])

  if (isLoading) {
    return (
      <div className="reader">
        <Loading label="Đang mở chương..." />
      </div>
    )
  }

  if (isError) {
    return (
      <div className="reader reader-message">
        {error?.response?.status === 404 ? (
          <Empty title="Không tìm thấy chương này" action={<Link className="btn" to={`/truyen/${slug}`}>Về trang truyện</Link>} />
        ) : (
          <ErrorBox error={error} onRetry={refetch} />
        )}
      </div>
    )
  }

  const totalPages = chapter.pages.length
  const progress = totalPages ? (currentPage / totalPages) * 100 : 0
  const label = chapterLabel(chapter.chapterNumber)

  return (
    <div className="reader">
      <header className={`reader-bar ${barHidden ? 'is-hidden' : ''}`}>
        <div className="reader-progress" style={{ width: `${progress}%` }} />
        <Link to={`/truyen/${slug}`} className="icon-btn reader-back" aria-label="Về trang truyện">
          <Icon name="left" />
        </Link>
        <div className="reader-title">
          <Link to={`/truyen/${slug}`}>{chapter.comicTitle}</Link>
          <span>
            Chương {label}
            {chapterTitleText(chapter) ? ` · ${chapterTitleText(chapter)}` : ''}
          </span>
        </div>
        <span className="reader-counter" aria-live="polite">
          {currentPage}/{totalPages}
        </span>
      </header>

      <main className="reader-pages" ref={pagesRef}>
        {chapter.pages.map((page) => (
          <div key={page.id} data-page={page.pageNumber} className="reader-page">
            <ChapterImage page={page} seedText={`${chapter.comicTitle}-${label}`} />
          </div>
        ))}
      </main>

      <div className="reader-end">
        <span className="burst-label">Hết chương {label}!</span>
        {chapter.nextChapter != null ? (
          <button className="btn btn-primary btn-large" onClick={() => goTo(chapter.nextChapter)}>
            Đọc chương {chapterLabel(chapter.nextChapter)} <Icon name="right" size={18} />
          </button>
        ) : (
          <p>Bạn đã đọc tới chương mới nhất. Theo dõi truyện để không bỏ lỡ chương sau nhé!</p>
        )}
      </div>

      <nav className={`reader-controls ${barHidden ? 'is-hidden' : ''}`} aria-label="Chuyển chương">
        <button className="icon-btn" disabled={chapter.prevChapter == null} onClick={() => goTo(chapter.prevChapter)} aria-label="Chương trước">
          <Icon name="left" />
        </button>
        <select value={label} onChange={(event) => goTo(event.target.value)} aria-label="Chọn chương">
          {chapters.map((item) => (
            <option key={item.id} value={chapterLabel(item.chapterNumber)}>
              Chương {chapterLabel(item.chapterNumber)}
            </option>
          ))}
        </select>
        <button className="icon-btn" disabled={chapter.nextChapter == null} onClick={() => goTo(chapter.nextChapter)} aria-label="Chương sau">
          <Icon name="right" />
        </button>
      </nav>

      <div className="reader-comments">
        <div className="panel">
          <Comments comicId={chapter.comicId} chapterId={chapter.id} />
        </div>
      </div>
    </div>
  )
}
