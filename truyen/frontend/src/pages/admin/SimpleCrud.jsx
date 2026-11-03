import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { errorMessage } from '../../api/client.js'
import { useToast } from '../../components/Toast.jsx'
import { Empty, ErrorBox, Field, Loading, Modal } from '../../components/Ui.jsx'
import Icon from '../../components/Icon.jsx'

/** Trang quản lý cho danh mục đơn giản có tên và mô tả: thể loại, tác giả. */
export default function SimpleCrud({ title, noun, queryKey, list, create, update, remove, textField, textLabel, textLimit, renderMeta }) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ name: '', text: '' })
  const items = useQuery({ queryKey, queryFn: list })

  const refresh = () => queryClient.invalidateQueries({ queryKey })
  const onError = (error) => toast.error(errorMessage(error))
  const save = useMutation({
    mutationFn: () => {
      const body = { name: form.name, [textField]: form.text || null }
      return editing === 'new' ? create(body) : update(editing.id, body)
    },
    onSuccess: () => {
      toast.success(`Đã lưu ${noun}.`)
      setEditing(null)
      refresh()
    },
    onError,
  })
  const del = useMutation({
    mutationFn: remove,
    onSuccess: () => {
      toast.success(`Đã xóa ${noun}.`)
      refresh()
    },
    onError,
  })

  const open = (item) => {
    setForm(item === 'new' ? { name: '', text: '' } : { name: item.name, text: item[textField] ?? '' })
    setEditing(item)
  }

  return (
    <>
      <div className="admin-head">
        <h1 className="admin-title">{title}</h1>
        <button className="btn btn-primary" onClick={() => open('new')}>
          <Icon name="plus" size={18} /> Thêm {noun}
        </button>
      </div>
      {items.isLoading ? (
        <Loading />
      ) : items.isError ? (
        <ErrorBox error={items.error} onRetry={items.refetch} />
      ) : items.data.length === 0 ? (
        <Empty title={`Chưa có ${noun} nào`} />
      ) : (
        <div className="crud-grid">
          {items.data.map((item) => (
            <article key={item.id} className="panel crud-card">
              <div>
                <h2>{item.name}</h2>
                {renderMeta?.(item)}
                <p>{item[textField] || <span className="muted">Chưa có {textLabel.toLowerCase()}</span>}</p>
              </div>
              <div className="table-actions">
                <button className="icon-btn" aria-label={`Sửa ${item.name}`} onClick={() => open(item)}>
                  <Icon name="edit" size={18} />
                </button>
                <button
                  className="icon-btn"
                  aria-label={`Xóa ${item.name}`}
                  onClick={() => {
                    if (window.confirm(`Xóa ${noun} "${item.name}"?`)) del.mutate(item.id)
                  }}
                >
                  <Icon name="trash" size={18} />
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
      {editing && (
        <Modal title={editing === 'new' ? `Thêm ${noun}` : `Sửa: ${editing.name}`} onClose={() => setEditing(null)}>
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault()
              save.mutate()
            }}
          >
            <Field label="Tên">
              <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} maxLength={100} required />
            </Field>
            <Field label={textLabel}>
              <textarea value={form.text} onChange={(event) => setForm({ ...form, text: event.target.value })} rows={4} maxLength={textLimit} />
            </Field>
            <div className="form-actions">
              <button type="button" className="btn" onClick={() => setEditing(null)}>
                Hủy
              </button>
              <button className="btn btn-primary" disabled={save.isPending}>
                Lưu
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  )
}
