import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { admin } from '../../api/index.js'
import { Empty, ErrorBox, Loading, Modal, Pagination } from '../../components/Ui.jsx'
import { formatDateTime } from '../../lib/format.js'

function AuditDetail({ id, onClose }) {
  const { data, isLoading, isError, error } = useQuery({ queryKey: ['admin', 'audit', id], queryFn: () => admin.auditLog(id) })
  return (
    <Modal title={`Nhật ký #${id}`} onClose={onClose}>
      {isLoading ? (
        <Loading />
      ) : isError ? (
        <ErrorBox error={error} />
      ) : (
        <div className="audit-detail">
          <p>
            <strong>{data.action}</strong> trên {data.targetType} #{data.targetId ?? '–'}, bởi user #{data.actorId}, lúc {formatDateTime(data.createdAt)}
          </p>
          <h4>Trước</h4>
          <pre>{data.beforeData ? JSON.stringify(data.beforeData, null, 2) : 'Không có'}</pre>
          <h4>Sau</h4>
          <pre>{data.afterData ? JSON.stringify(data.afterData, null, 2) : 'Không có'}</pre>
        </div>
      )}
    </Modal>
  )
}

export default function AdminAudit() {
  const [action, setAction] = useState('')
  const [page, setPage] = useState(0)
  const [openId, setOpenId] = useState(null)
  const list = useQuery({
    queryKey: ['admin', 'audit', 'list', action, page],
    queryFn: () => admin.auditLogs({ action: action.trim() || undefined, page, size: 20 }),
    placeholderData: keepPreviousData,
  })
  return (
    <>
      <h1 className="admin-title">Nhật ký thao tác</h1>
      <input
        className="admin-search"
        type="search"
        value={action}
        onChange={(event) => {
          setAction(event.target.value.toUpperCase())
          setPage(0)
        }}
        placeholder="Lọc đúng tên thao tác, ví dụ CHAPTER_PUBLISH"
        aria-label="Lọc theo thao tác"
      />
      {list.isLoading ? (
        <Loading />
      ) : list.isError ? (
        <ErrorBox error={list.error} onRetry={list.refetch} />
      ) : list.data.items.length === 0 ? (
        <Empty title="Chưa có nhật ký nào">Các thao tác của admin sẽ được ghi lại ở đây.</Empty>
      ) : (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Thao tác</th>
                  <th>Đối tượng</th>
                  <th>Người thực hiện</th>
                  <th aria-label="Chi tiết" />
                </tr>
              </thead>
              <tbody>
                {list.data.items.map((item) => (
                  <tr key={item.id}>
                    <td>{formatDateTime(item.createdAt)}</td>
                    <td>
                      <code>{item.action}</code>
                    </td>
                    <td>
                      {item.targetType} #{item.targetId ?? '–'}
                    </td>
                    <td>User #{item.actorId}</td>
                    <td>
                      <button className="btn btn-small" onClick={() => setOpenId(item.id)}>
                        Chi tiết
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />
        </>
      )}
      {openId && <AuditDetail id={openId} onClose={() => setOpenId(null)} />}
    </>
  )
}
