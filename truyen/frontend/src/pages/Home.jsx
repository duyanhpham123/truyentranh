import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { catalog, comics, me } from '../api/index.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { Cover } from '../components/ComicArt.jsx'
import { ComicGrid, ComicGridSkeleton } from '../components/ComicCard.jsx'
import { ErrorBox, Empty, SectionTitle } from '../components/Ui.jsx'
import Icon from '../components/Icon.jsx'
import { guestHistory } from '../lib/guestHistory.js'
import { chapterLabel, compactNumber, STATUS_LABEL } from '../lib/format.js'

function Hero({ items }) {
  const [index, setIndex] = useState(0)
  const [paused, setPaused] = useState(false)
  const count = items.length

  useEffect(() => {
    if (paused || count < 2 || matchMedia('(prefers-reduced-motion: reduce)').matches) return undefined
    const timer = setInterval(() => setIndex((value) => (value + 1) % count), 6000)
    return () => clearInterval(timer)
  }, [paused, count])

  const comic = items[index % count]
  return (
    <section className="hero" onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)} aria-label="Truyện nổi bật">
      <div className="hero-rays" aria-hidden="true" />
      <div className="container hero-inner">
        <div className="hero-copy" key={comic.id}>
          <span className="burst-label">Hot tuần này!</span>
          <h1>{comic.title}</h1>
          <div className="hero-tags">
            {comic.genres.slice(0, 3).map((genre) => (
              <Link key={genre.id} to={`/truyen?genre=${genre.slug}`} className="chip">
                {genre.name}
              </Link>
            ))}
            <span className="chip chip-plain">{STATUS_LABEL[comic.status]}</span>
          </div>
          <div className="hero-stats">
            <span>
              <Icon name="eye" size={18} /> {compactNumber(comic.views)} lượt xem
            </span>
            {comic.ratingCount > 0 && (
              <span>
                <Icon name="star" size={18} filled /> {Number(comic.ratingAvg).toFixed(1)}
              </span>
            )}
            {comic.latestChapter != null && <span>Chương {chapterLabel(comic.latestChapter)}</span>}
          </div>
          <div className="hero-actions">
            <Link className="btn btn-primary btn-large" to={`/truyen/${comic.slug}`}>
              Đọc ngay <Icon name="right" size={18} />
            </Link>
            <Link className="btn btn-large" to="/truyen">
              Khám phá thêm
            </Link>
          </div>
        </div>
        <Link to={`/truyen/${comic.slug}`} className="hero-cover" key={`cover-${comic.id}`} aria-label={`Đọc ${comic.title}`}>
          <Cover url={comic.coverUrl} title={comic.title} />
        </Link>
      </div>
      <div className="container hero-dots">
        {items.map((item, itemIndex) => (
          <button
            key={item.id}
            className={itemIndex === index % count ? 'is-active' : ''}
            onClick={() => setIndex(itemIndex)}
            aria-label={`Xem truyện ${item.title}`}
            aria-current={itemIndex === index % count}
          />
        ))}
      </div>
    </section>
  )
}

function Ticker({ comics: items }) {
  if (!items?.length) return null
  const entries = items.slice(0, 10)
  return (
    <div className="ticker" aria-label="Mới cập nhật">
      <span className="ticker-label">Mới ra lò</span>
      <div className="ticker-track">
        {[0, 1].map((copy) => (
          <div className="ticker-group" key={copy} aria-hidden={copy === 1}>
            {entries.map((comic) => (
              <Link key={comic.id} to={`/truyen/${comic.slug}`} tabIndex={copy === 1 ? -1 : undefined}>
                <strong>{comic.title}</strong> · Chương {chapterLabel(comic.latestChapter)}
              </Link>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}

const PERIODS = [
  { value: 'day', label: 'Ngày' },
  { value: 'week', label: 'Tuần' },
  { value: 'month', label: 'Tháng' },
]

function HotRanking() {
  const [period, setPeriod] = useState('week')
  const { data = [], isLoading } = useQuery({ queryKey: ['hot', period], queryFn: () => comics.hot(period) })
  return (
    <aside className="ranking" aria-label="Bảng xếp hạng">
      <div className="ranking-head">
        <h2>
          <Icon name="fire" size={24} filled /> Bảng xếp hạng
        </h2>
        <div className="tabs" role="tablist">
          {PERIODS.map((item) => (
            <button key={item.value} role="tab" aria-selected={period === item.value} className={period === item.value ? 'is-active' : ''} onClick={() => setPeriod(item.value)}>
              {item.label}
            </button>
          ))}
        </div>
      </div>
      {isLoading ? (
        <div className="skeleton skeleton-ranking" />
      ) : (
        <ol className="ranking-list">
          {data.slice(0, 7).map((comic, index) => (
            <li key={comic.id}>
              <Link to={`/truyen/${comic.slug}`}>
                <span className={`rank rank-${index + 1}`}>{index + 1}</span>
                <Cover url={comic.coverUrl} title={comic.title} className="ranking-cover" />
                <span className="ranking-info">
                  <strong>{comic.title}</strong>
                  <small>
                    <Icon name="eye" size={13} /> {compactNumber(comic.views)}
                    {comic.latestChapter != null && ` · Chương ${chapterLabel(comic.latestChapter)}`}
                  </small>
                </span>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </aside>
  )
}

function ContinueReading() {
  const { user, ready } = useAuth()
  const { data } = useQuery({
    queryKey: ['history', 'home'],
    queryFn: () => me.history({ size: 6 }),
    enabled: ready && Boolean(user),
  })
  const items = user
    ? (data?.items ?? []).map((item) => ({ ...item, title: item.comicTitle, slug: item.comicSlug }))
    : guestHistory().slice(0, 6).map((item) => ({ ...item, title: item.comicTitle, slug: item.comicSlug }))
  if (!items.length) return null
  return (
    <section className="container section">
      <SectionTitle icon="book" action={<Link to="/tu-truyen" className="link-more">Xem tất cả</Link>}>
        Đọc tiếp nào
      </SectionTitle>
      <div className="continue-row">
        {items.map((item) => (
          <Link key={item.comicId} to={`/truyen/${item.slug}/chuong/${chapterLabel(item.chapterNumber)}`} className="continue-card">
            <Cover url={item.coverUrl} title={item.title} />
            <span>
              <strong>{item.title}</strong>
              <small>Chương {chapterLabel(item.chapterNumber)}</small>
            </span>
          </Link>
        ))}
      </div>
    </section>
  )
}

export default function Home() {
  const hot = useQuery({ queryKey: ['hot', 'week'], queryFn: () => comics.hot('week') })
  const latest = useQuery({ queryKey: ['comics', 'latest'], queryFn: () => comics.search({ sort: 'latest', size: 24 }) })
  const topRated = useQuery({ queryKey: ['comics', 'rating'], queryFn: () => comics.search({ sort: 'rating', size: 6 }) })
  const genres = useQuery({ queryKey: ['genres'], queryFn: catalog.genres, staleTime: 300_000 })

  // Truyện chưa có chương nào thì không đưa lên hero
  const heroItems = (hot.data ?? []).filter((comic) => comic.latestChapter != null).slice(0, 5)

  return (
    <>
      {heroItems.length > 0 ? <Hero items={heroItems} /> : hot.isLoading && <div className="hero hero-skeleton" />}
      <Ticker comics={latest.data?.items.filter((comic) => comic.latestChapter != null)} />
      <ContinueReading />

      <div className="container home-columns">
        <section className="section">
          <SectionTitle icon="grid" action={<Link to="/truyen" className="link-more">Xem tất cả</Link>}>
            Mới cập nhật
          </SectionTitle>
          {latest.isLoading ? (
            <ComicGridSkeleton />
          ) : latest.isError ? (
            <ErrorBox error={latest.error} onRetry={latest.refetch} />
          ) : latest.data.items.length ? (
            <ComicGrid comics={latest.data.items} />
          ) : (
            <Empty title="Chưa có truyện nào" />
          )}
        </section>
        <HotRanking />
      </div>

      {genres.data?.length > 0 && (
        <section className="genre-band">
          <div className="container">
            <h2>Hôm nay đọc gì?</h2>
            <div className="genre-band-list">
              {genres.data.map((genre, index) => (
                <Link key={genre.id} to={`/truyen?genre=${genre.slug}`} className={`genre-tile genre-tile-${index % 4}`}>
                  {genre.name}
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {topRated.data?.items.length > 0 && (
        <section className="container section">
          <SectionTitle icon="star" action={<Link to="/truyen?sort=rating" className="link-more">Xem tất cả</Link>}>
            Độc giả chấm cao
          </SectionTitle>
          <ComicGrid comics={topRated.data.items} />
        </section>
      )}
    </>
  )
}
