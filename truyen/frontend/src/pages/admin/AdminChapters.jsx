import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { admin } from '../../api/index.js'
import { errorMessage } from '../../api/client.js'
import { ChapterImage } from '../../components/ComicArt.jsx'
import { useToast } from '../../components/Toast.jsx'
import { Empty, ErrorBox, Field, Loading, Modal } from '../../components/Ui.jsx'
import Icon from '../../components/Icon.jsx'
import { chapterLabel, formatDateTime } from '../../lib/format.js'

function ChapterForm({ comicId, chapter, onClose, onSaved }) {
  const toast = useToast()
  const [number, setNumber] = useState(chapter ? chapterLabel(chapter.chapterNumber) : '')
  const [title, setTitle] = useState(chapter?.chapterTitle ?? '')
  const save = useMutation({
    mutationFn: () => {
      const body = { chapterNumber: Number(number), chapterTitle: title || null }
      return chapter ? admin.updateChapter(chapter.id, body) : admin.createChapter(comicId, body)
    },
    onSuccess: onSaved,
    onError: (error) => toast.error(errorMessage(error)),
  })
  return (
    <Modal title={chapter ? `Sửa chương ${chapterLabel(chapter.chapterNumber)}` : 'Thêm chương (bản nháp)'} onClose={onClose}>
      <form
        className="form"
        onSubmit={(event) => {
          event.preventDefault()
          save.mutate()
        }}
      >
        <Field label="Số chương" hint="Có thể dùng số lẻ, ví dụ 10.5">
          <input type="number" step="0.01" min="0.01" value={number} onChange={(event) => setNumber(event.target.value)} required />
        </Field>
        <Field label="Tên chương">
          <input value={title} onChange={(event) => setTitle(event.target.value)} maxLength={255} />
        </Field>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Hủy
          </button>
          <button className="btn btn-primary" disabled={save.isPending}>
            Lưu
          </button>
        </div>
      </form>
    </Modal>
  )
}

function PageManager({ chapter, comicTitle, onChanged }) {
  const toast = useToast()
  const fileRef = useRef(null)
  const [pages, setPages] = useState(chapter.pages)
  useEffect(() => setPages(chapter.pages), [chapter.pages])
  const onError = (error) => toast.error(errorMessage(error))

  const upload = useMutation({
    mutationFn: (files) => admin.uploadPages(chapter.id, files),
    onSuccess: () => {
      toast.success('Đã tải ảnh lên.')
      onChanged()
    },
    onError,
  })
  const reorder = useMutation({
    mutationFn: (ids) => admin.reorderPages(chapter.id, ids),
    onSuccess: () => {
      toast.success('Đã lưu thứ tự trang.')
      onChanged()
    },
    onError,
  })

  const move = (index, delta) => {
    const next = [...pages]
    const target = index + delta
    if (target < 0 || target >= next.length) return
    ;[next[index], next[target]] = [next[target], next[index]]
    setPages(next)
  }
  const dirty = pages.map((page) => page.id).join() !== chapter.pages.map((page) => page.id).join()

  return (
    <div className="page-manager">
      <div className="page-manager-tools">
        <button className="btn btn-small" disabled={upload.isPending} onClick={() => fileRef.current.click()}>
          <Icon name="upload" size={16} /> {upload.isPending ? 'Đang tải lên...' : 'Tải trang lên'}
        </button>
        <input
          ref={fileRef}
          type="file"
          multiple
          accept="image/jpeg,image/png,image/webp"
          hidden
          aria-label="Chọn ảnh trang truyện"
          onChange={(event) => {
            const files = [...event.target.files].sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true }))
            if (files.length) upload.mutate(files)
            event.target.value = ''
          }}
        />
        {dirty && pages.length > 0 && (
          <button className="btn btn-primary btn-small" disabled={reorder.isPending} onClick={() => reorder.mutate(pages.map((page) => page.id))}>
            Lưu thay đổi trang
          </button>
        )}
        <span className="muted">Ảnh được xếp theo tên file. JPEG, PNG hoặc WEBP.</span>
      </div>
      {pages.length === 0 ? (
        <p className="muted">Chưa có trang nào.</p>
      ) : (
        <ol className="page-thumbs">
          {pages.map((page, index) => (
            <li key={page.id}>
              <div className="page-thumb">
                <ChapterImage page={page} seedText={`${comicTitle}-${chapterLabel(chapter.chapterNumber)}`} />
                <span className="page-thumb-number">{index + 1}</span>
              </div>
              <div className="page-thumb-actions">
                <button className="icon-btn icon-btn-small" disabled={index === 0} onClick={() => move(index, -1)} aria-label={`Đưa trang ${index + 1} lên trước`}>
                  <Icon name="left" size={15} />
                </button>
                <button className="icon-btn icon-btn-small" disabled={index === pages.length - 1} onClick={() => move(index, 1)} aria-label={`Đưa trang ${index + 1} ra sau`}>
                  <Icon name="right" size={15} />
                </button>
                <button
                  className="icon-btn icon-btn-small"
                  disabled={pages.length === 1}
                  onClick={() => setPages(pages.filter((other) => other.id !== page.id))}
                  aria-label={`Bỏ trang ${index + 1}`}
                >
                  <Icon name="trash" size={15} />
                </button>
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}

export default function AdminChapters() {
  const { comicId } = useParams()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(null)
  const [openId, setOpenId] = useState(null)

  const { data: comicPage } = useQuery({ queryKey: ['admin', 'comics', 'all'], queryFn: () => admin.comics({ size: 100 }) })
  const comic = comicPage?.items.find((item) => String(item.id) === comicId)
  const chapters = useQuery({ queryKey: ['admin', 'chapters', comicId], queryFn: () => admin.chapters(comicId) })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['admin', 'chapters', comicId] })
    if (comic) {
      queryClient.invalidateQueries({ queryKey: ['chapters', comic.slug] })
      queryClient.invalidateQueries({ queryKey: ['chapter', comic.slug] })
    }
  }
  const onError = (error) => toast.error(errorMessage(error))
  const publish = useMutation({
    mutationFn: admin.publish,
    onSuccess: () => {
      toast.success('Đã đăng chương! Người theo dõi sẽ nhận được thông báo.')
      refresh()
    },
    onError,
  })
  const remove = useMutation({
    mutationFn: admin.deleteChapter,
    onSuccess: () => {
      toast.success('Đã xóa chương.')
      refresh()
    },
    onError,
  })

  const title = comic?.title ?? 'Truyện'

  return (
    <>
      <Link to="/admin/truyen" className="back-link">
        <Icon name="left" size={16} /> Danh sách truyện
      </Link>
      <div className="admin-head">
        <h1 className="admin-title">{title}</h1>
        <button className="btn btn-primary" onClick={() => setEditing('new')}>
          <Icon name="plus" size={18} /> Thêm chương
        </button>
      </div>
      {chapters.isLoading ? (
        <Loading />
      ) : chapters.isError ? (
        <ErrorBox error={chapters.error} onRetry={chapters.refetch} />
      ) : chapters.data.length === 0 ? (
        <Empty title="Chưa có chương nào">Tạo chương nháp, tải trang lên rồi bấm Đăng.</Empty>
      ) : (
        <ul className="admin-chapters">
          {chapters.data.map((chapter) => (
            <li key={chapter.id} className="panel admin-chapter">
              <div className="admin-chapter-head">
                <button
                  className="admin-chapter-title"
                  aria-expanded={openId === chapter.id}
                  onClick={() => setOpenId(openId === chapter.id ? null : chapter.id)}
                >
                  <Icon name={openId === chapter.id ? 'down' : 'right'} size={16} />
                  <strong>Chương {chapterLabel(chapter.chapterNumber)}</strong>
                  <span>{chapter.chapterTitle}</span>
                </button>
                <span className={`status-pill ${chapter.status === 'PUBLISHED' ? 'status-good' : 'status-draft'}`}>{chapter.status === 'PUBLISHED' ? 'Đã đăng' : 'Bản nháp'}</span>
                <span className="muted admin-chapter-meta">
                  {chapter.pages.length} trang{chapter.publishedAt ? ` · ${formatDateTime(chapter.publishedAt)}` : ''}
                </span>
                <div className="table-actions">
                  {chapter.status === 'DRAFT' && (
                    <button className="btn btn-primary btn-small" disabled={publish.isPending} onClick={() => publish.mutate(chapter.id)}>
                      Đăng
                    </button>
                  )}
                  <button className="icon-btn" aria-label={`Sửa chương ${chapterLabel(chapter.chapterNumber)}`} onClick={() => setEditing(chapter)}>
                    <Icon name="edit" size={18} />
                  </button>
                  <button
                    className="icon-btn"
                    aria-label={`Xóa chương ${chapterLabel(chapter.chapterNumber)}`}
                    onClick={() => {
                      if (window.confirm(`Xóa chương ${chapterLabel(chapter.chapterNumber)}?`)) remove.mutate(chapter.id)
                    }}
                  >
                    <Icon name="trash" size={18} />
                  </button>
                </div>
              </div>
              {openId === chapter.id && <PageManager chapter={chapter} comicTitle={title} onChanged={refresh} />}
            </li>
          ))}
        </ul>
      )}
      {editing && (
        <ChapterForm
          comicId={comicId}
          chapter={editing === 'new' ? null : editing}
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
