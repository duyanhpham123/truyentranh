import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { admin } from '../../api/index.js'
import { errorMessage } from '../../api/client.js'
import { useToast } from '../../components/Toast.jsx'
import { Empty, ErrorBox, Loading, Pagination } from '../../components/Ui.jsx'
import { timeAgo } from '../../lib/format.js'

const TABS = [
  { value: 'PENDING', label: 'Chờ xử lý' },
  { value: 'RESOLVED', label: 'Đã xử lý' },
  { value: 'DISMISSED', label: 'Đã bỏ qua' },
]

export default function AdminReports() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [status, setStatus] = useState('PENDING')
  const [page, setPage] = useState(0)
  const list = useQuery({ queryKey: ['admin', 'reports', status, page], queryFn: () => admin.reports({ status, page, size: 15 }) })
  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['admin', 'reports'] })
    queryClient.invalidateQueries({ queryKey: ['admin', 'summary'] })
  }
  const onError = (error) => toast.error(errorMessage(error))
  const dismiss = useMutation({
    mutationFn: (id) => admin.resolveReport(id, 'DISMISSED'),
    onSuccess: () => {
      toast.success('Đã bỏ qua báo cáo.')
      refresh()
    },
    onError,
  })
  const hide = useMutation({
    mutationFn: admin.hideComment,
    onSuccess: () => {
      toast.success('Đã ẩn bình luận.')
      refresh()
    },
    onError,
  })

  return (
    <>
      <h1 className="admin-title">Báo cáo bình luận</h1>
      <div className="tabs tabs-spaced" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.value}
            role="tab"
            aria-selected={status === tab.value}
            className={status === tab.value ? 'is-active' : ''}
            onClick={() => {
              setStatus(tab.value)
              setPage(0)
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>
      {list.isLoading ? (
        <Loading />
      ) : list.isError ? (
        <ErrorBox error={list.error} onRetry={list.refetch} />
      ) : list.data.items.length === 0 ? (
        <Empty title="Không có báo cáo nào" />
      ) : (
        <>
          <ul className="report-list">
            {list.data.items.map((report) => (
              <li key={report.id} className="panel report">
                <blockquote>{report.commentContent}</blockquote>
                <p>
                  <strong>Lý do:</strong> {report.reason}
                </p>
                <p className="muted">
                  {report.reporterName} báo cáo {timeAgo(report.createdAt)}
                </p>
                {status === 'PENDING' && (
                  <div className="form-actions form-actions-left">
                    <button
                      className="btn btn-primary btn-small"
                      disabled={hide.isPending}
                      onClick={() => {
                        if (window.confirm('Ẩn bình luận này?')) hide.mutate(report.commentId)
                      }}
                    >
                      Ẩn bình luận
                    </button>
                    <button className="btn btn-small" disabled={dismiss.isPending} onClick={() => dismiss.mutate(report.id)}>
                      Bỏ qua
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
          <Pagination page={page} totalPages={list.data.totalPages} onChange={setPage} />
        </>
      )}
    </>
  )
}
