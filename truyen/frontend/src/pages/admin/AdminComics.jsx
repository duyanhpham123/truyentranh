import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { admin, catalog } from '../../api/index.js'
import { errorMessage } from '../../api/client.js'
import { Cover } from '../../components/ComicArt.jsx'
import { useToast } from '../../components/Toast.jsx'
import { Empty, ErrorBox, Field, Loading, Modal, Pagination } from '../../components/Ui.jsx'
import Icon from '../../components/Icon.jsx'
import { compactNumber, STATUS_LABEL } from '../../lib/format.js'

function ComicForm({ comic, onClose, onSaved }) {
  const toast = useToast()
  const { data: genres = [] } = useQuery({ queryKey: ['genres'], queryFn: catalog.genres })
  const { data: authors = [] } = useQuery({ queryKey: ['authors'], queryFn: catalog.authors })
  const [form, setForm] = useState({
    title: comic?.title ?? '',
    authorId: comic?.authorId ?? '',
    description: comic?.description ?? '',
    status: comic?.status ?? 'ONGOING',
    genreIds: comic?.genres.map((genre) => genre.id) ?? [],
  })
  const save = useMutation({
    mutationFn: () => {
      const body = { ...form, authorId: form.authorId === '' ? null : Number(form.authorId) }
      return comic ? admin.updateComic(comic.id, body) : admin.createComic(body)
    },
    onSuccess: () => {
      toast.success(comic ? 'Đã lưu truyện.' : 'Đã tạo truyện.')
      onSaved()
    },
    onError: (error) => toast.error(errorMessage(error)),
  })
  const toggleGenre = (id) =>
    setForm((current) => ({
      ...current,
      genreIds: current.genreIds.includes(id) ? current.genreIds.filter((value) => value !== id) : [...current.genreIds, id],
    }))

  return (
    <Modal title={comic ? `Sửa: ${comic.title}` : 'Thêm truyện mới'} onClose={onClose}>
      <form
        className="form"
        onSubmit={(event) => {
          event.preventDefault()
          save.mutate()
        }}
      >
        <Field label="Tên truyện">
          <input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} maxLength={255} required />
        </Field>
        <div className="form-row">
          <Field label="Tác giả">
            <select value={form.authorId} onChange={(event) => setForm({ ...form, authorId: event.target.value })}>
              <option value="">Không rõ</option>
              {authors.map((author) => (
                <option key={author.id} value={author.id}>
                  {author.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Trạng thái">
            <select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>
              {Object.entries(STATUS_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <Field label="Nội dung">
          <textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} rows={4} />
        </Field>
        <fieldset className="genre-picker">
          <legend>Thể loại</legend>
          <div className="chip-row">
            {genres.map((genre) => (
              <button type="button" key={genre.id} className={`chip ${form.genreIds.includes(genre.id) ? 'is-active' : ''}`} aria-pressed={form.genreIds.includes(genre.id)} onClick={() => toggleGenre(genre.id)}>
                {genre.name}
              </button>
            ))}
          </div>
        </fieldset>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Hủy
          </button>
          <button className="btn btn-primary" disabled={save.isPending}>
            {save.isPending ? 'Đang lưu...' : 'Lưu'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default function AdminComics() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState(null)
  const coverInput = useRef(null)
  const coverTarget = useRef(null)

  const list = useQuery({
    queryKey: ['admin', 'comics', keyword, page],
    queryFn: () => admin.comics({ keyword: keyword || undefined, page, size: 15 }),
    placeholderData: keepPreviousData,
  })
  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['admin', 'comics'] })
    queryClient.invalidateQueries({ queryKey: ['comics'] })
    queryClient.invalidateQueries({ queryKey: ['comic'] })
  }
  const onError = (error) => toast.error(errorMessage(error))
  const hide = useMutation({ mutationFn: ({ id, hidden }) => admin.setHidden(id, hidden), onSuccess: refresh, onError })
  const remove = useMutation({
    mutationFn: admin.deleteComic,
    onSuccess: () => {
      toast.success('Đã xóa truyện.')
      refresh()
    },
    onError,
  })
  const cover = useMutation({
    mutationFn: ({ id, file }) => admin.cover(id, file),
    onSuccess: () => {
      toast.success('Đã đổi ảnh bìa.')
      refresh()
    },
    onError,
  })

  return (
    <>
      <div className="admin-head">
        <h1 className="admin-title">Truyện</h1>
        <button className="btn btn-primary" onClick={() => setEditing('new')}>
          <Icon name="plus" size={18} /> Thêm truyện
        </button>
      </div>
      <input
        className="admin-search"
        type="search"
        value={keyword}
        onChange={(event) => {
          setKeyword(event.target.value)
          setPage(0)
        }}
        placeholder="Tìm theo tên truyện..."
        aria-label="Tìm truyện"
      />
      <input
        ref={coverInput}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        hidden
        aria-label="Chọn ảnh bìa"
        onChange={(event) => {
          const file = event.target.files[0]
          if (file) cover.mutate({ id: coverTarget.current, file })
          event.target.value = ''
        }}
      />
      {list.isLoading ? (
        <Loading />
      ) : list.isError ? (
        <ErrorBox error={list.error} onRetry={list.refetch} />
      ) : list.data.items.length === 0 ? (
        <Empty title="Không có truyện nào" />
      ) : (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Truyện</th>
                  <th>Tác giả</th>
                  <th>Trạng thái</th>
                  <th className="num">Lượt xem</th>
                  <th>Hiển thị</th>
                  <th aria-label="Thao tác" />
                </tr>
              </thead>
              <tbody>
                {list.data.items.map((comic) => (
                  <tr key={comic.id}>
                    <td>
                      <div className="table-comic">
                        <Cover url={comic.coverUrl} title={comic.title} />
                        <Link to={`/admin/truyen/${comic.id}/chuong`}>{comic.title}</Link>
                      </div>
                    </td>
                    <td>{comic.authorName ?? '–'}</td>
                    <td>{STATUS_LABEL[comic.status]}</td>
                    <td className="num">{compactNumber(comic.views)}</td>
                    <td>
                      <button className={`toggle ${comic.hidden ? '' : 'is-on'}`} aria-pressed={!comic.hidden} onClick={() => hide.mutate({ id: comic.id, hidden: !comic.hidden })}>
                        {comic.hidden ? 'Đang ẩn' : 'Đang hiện'}
                      </button>
                    </td>
                    <td>
                      <div className="table-actions">
                        <Link className="btn btn-small" to={`/admin/truyen/${comic.id}/chuong`}>
                          Chương
                        </Link>
                        <button
                          className="icon-btn"
                          aria-label={`Đổi ảnh bìa ${comic.title}`}
                          title="Đổi ảnh bìa"
                          onClick={() => {
                            coverTarget.current = comic.id
                            coverInput.current.click()
                          }}
                        >
                          <Icon name="upload" size={18} />
                        </button>
                        <button className="icon-btn" aria-label={`Sửa ${comic.title}`} title="Sửa" onClick={() => setEditing(comic)}>
                          <Icon name="edit" size={18} />
                        </button>
                        <button
                          className="icon-btn"
                          aria-label={`Xóa ${comic.title}`}
                          title="Xóa"
                          onClick={() => {
                            if (window.confirm(`Xóa truyện "${comic.title}"?`)) remove.mutate(comic.id)
                          }}
                        >
                          <Icon name="trash" size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />
        </>
      )}
      {editing && (
        <ComicForm
          comic={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            refresh()
          }}
        />
      )}
    </>
  )
}
