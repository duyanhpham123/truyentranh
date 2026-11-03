import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { catalog, comics } from '../api/index.js'
import { ComicGrid, ComicGridSkeleton } from '../components/ComicCard.jsx'
import { Empty, ErrorBox, Pagination } from '../components/Ui.jsx'
import Icon from '../components/Icon.jsx'

const SORTS = [
  { value: 'latest', label: 'Mới cập nhật' },
  { value: 'views', label: 'Xem nhiều' },
  { value: 'rating', label: 'Đánh giá cao' },
]

const STATUSES = [
  { value: '', label: 'Mọi trạng thái' },
  { value: 'ONGOING', label: 'Đang ra' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'HIATUS', label: 'Tạm ngưng' },
]

const PAGE_SIZE = 18

export default function Browse() {
  const [params, setParams] = useSearchParams()
  const keyword = params.get('keyword') ?? ''
  const genre = params.get('genre') ?? ''
  const status = params.get('status') ?? ''
  const sort = params.get('sort') ?? 'latest'
  const authorId = params.get('authorId') ?? ''
  const page = Number(params.get('page') ?? 0)

  const [input, setInput] = useState(keyword)
  useEffect(() => setInput(keyword), [keyword])

  const update = (changes) => {
    const next = new URLSearchParams(params)
    for (const [key, value] of Object.entries(changes)) {
      if (value === '' || value == null) next.delete(key)
      else next.set(key, value)
    }
    if (!('page' in changes)) next.delete('page')
    setParams(next)
  }

  useEffect(() => {
    if (input === keyword) return undefined
    const timer = setTimeout(() => update({ keyword: input.trim() }), 350)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [input])

  const { data: genres = [] } = useQuery({ queryKey: ['genres'], queryFn: catalog.genres, staleTime: 300_000 })
  const { data: author } = useQuery({ queryKey: ['author', authorId], queryFn: () => catalog.author(authorId), enabled: Boolean(authorId) })
  const result = useQuery({
    queryKey: ['comics', 'browse', { keyword, genre, status, sort, authorId, page }],
    queryFn: () =>
      comics.search({ keyword: keyword || undefined, genre: genre || undefined, status: status || undefined, authorId: authorId || undefined, sort, page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })

  const activeGenre = genres.find((item) => item.slug === genre)
  const title = keyword ? `Tìm: “${keyword}”` : author ? author.name : activeGenre ? activeGenre.name : 'Tất cả truyện'

  return (
    <>
      <div className="page-banner">
        <div className="container">
          <h1>{title}</h1>
          <p>{author?.biography ?? activeGenre?.description ?? (result.data ? `${result.data.totalItems} truyện đang chờ bạn` : ' ')}</p>
        </div>
      </div>
      <div className="container section">
        <div className="filters">
          <label className="filter-search">
            <Icon name="search" size={18} />
            <input type="search" value={input} onChange={(event) => setInput(event.target.value)} placeholder="Tên truyện hoặc tác giả" aria-label="Tìm theo tên truyện hoặc tác giả" />
          </label>
          <select value={status} onChange={(event) => update({ status: event.target.value })} aria-label="Lọc theo trạng thái">
            {STATUSES.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
          <div className="tabs" role="tablist" aria-label="Sắp xếp">
            {SORTS.map((item) => (
              <button key={item.value} role="tab" aria-selected={sort === item.value} className={sort === item.value ? 'is-active' : ''} onClick={() => update({ sort: item.value })}>
                {item.label}
              </button>
            ))}
          </div>
        </div>
        <div className="chip-row">
          <button className={`chip ${!genre ? 'is-active' : ''}`} onClick={() => update({ genre: '' })}>
            Tất cả
          </button>
          {genres.map((item) => (
            <button key={item.id} className={`chip ${genre === item.slug ? 'is-active' : ''}`} onClick={() => update({ genre: item.slug })}>
              {item.name}
            </button>
          ))}
          {authorId && (
            <button className="chip is-active" onClick={() => update({ authorId: '' })}>
              {author?.name ?? 'Tác giả'} <Icon name="x" size={14} />
            </button>
          )}
        </div>

        {result.isLoading ? (
          <ComicGridSkeleton count={PAGE_SIZE} />
        ) : result.isError ? (
          <ErrorBox error={result.error} onRetry={result.refetch} />
        ) : result.data.items.length ? (
          <>
            <ComicGrid comics={result.data.items} />
            <Pagination page={result.data.page} totalPages={result.data.totalPages} onChange={(value) => update({ page: value || '' })} />
          </>
        ) : (
          <Empty title="Không tìm thấy truyện nào" action={<Link className="btn" to="/truyen">Xóa bộ lọc</Link>}>
            Thử từ khóa khác hoặc bỏ bớt bộ lọc xem sao.
          </Empty>
        )}
      </div>
    </>
  )
}
