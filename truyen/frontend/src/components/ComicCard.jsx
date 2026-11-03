import { Link } from 'react-router-dom'
import { Cover } from './ComicArt.jsx'
import Icon from './Icon.jsx'
import { chapterLabel, compactNumber, STATUS_LABEL, timeAgo } from '../lib/format.js'

export function ComicCard({ comic }) {
  return (
    <article className="comic-card">
      <Link to={`/truyen/${comic.slug}`} className="comic-card-link">
        <div className="comic-card-cover">
          <Cover url={comic.coverUrl} title={comic.title} />
          {comic.status === 'COMPLETED' && <span className="sticker sticker-green">Full</span>}
          {comic.status === 'HIATUS' && <span className="sticker sticker-dark">{STATUS_LABEL.HIATUS}</span>}
          {comic.latestChapter != null && <span className="comic-card-chapter">Chương {chapterLabel(comic.latestChapter)}</span>}
        </div>
        <h3 className="comic-card-title">{comic.title}</h3>
      </Link>
      <div className="comic-card-meta">
        <span title={`${comic.views} lượt xem`}>
          <Icon name="eye" size={14} /> {compactNumber(comic.views)}
        </span>
        {comic.ratingCount > 0 && (
          <span title={`${comic.ratingCount} lượt đánh giá`}>
            <Icon name="star" size={14} filled /> {Number(comic.ratingAvg).toFixed(1)}
          </span>
        )}
        {comic.lastChapterAt && <span className="comic-card-time">{timeAgo(comic.lastChapterAt)}</span>}
      </div>
    </article>
  )
}

export function ComicGrid({ comics }) {
  return (
    <div className="comic-grid">
      {comics.map((comic) => (
        <ComicCard key={comic.id} comic={comic} />
      ))}
    </div>
  )
}

export function ComicGridSkeleton({ count = 12 }) {
  return (
    <div className="comic-grid" aria-hidden="true">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="skeleton-card">
          <div className="skeleton skeleton-cover" />
          <div className="skeleton skeleton-line" />
        </div>
      ))}
    </div>
  )
}
