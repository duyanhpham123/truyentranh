import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { admin } from '../../api/index.js'
import { errorMessage } from '../../api/client.js'
import { useAuth } from '../../auth/AuthContext.jsx'
import { useToast } from '../../components/Toast.jsx'
import { Avatar, Empty, ErrorBox, Loading, Pagination } from '../../components/Ui.jsx'
import { formatDate } from '../../lib/format.js'

const STATUS = { ACTIVE: 'Hoạt động', INACTIVE: 'Ngưng dùng', BANNED: 'Bị khóa' }

export default function AdminUsers() {
  const { user: currentUser } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const list = useQuery({
    queryKey: ['admin', 'users', keyword, page],
    queryFn: () => admin.users({ keyword: keyword || undefined, page, size: 15 }),
    placeholderData: keepPreviousData,
  })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
  const options = {
    onSuccess: () => {
      toast.success('Đã cập nhật người dùng.')
      refresh()
    },
    onError: (error) => toast.error(errorMessage(error)),
  }
  const setStatus = useMutation({ mutationFn: ({ id, status }) => admin.setUserStatus(id, status), ...options })
  const setRole = useMutation({ mutationFn: ({ id, role }) => admin.setUserRole(id, role), ...options })

  return (
    <>
      <h1 className="admin-title">Người dùng</h1>
      <input
        className="admin-search"
        type="search"
        value={keyword}
        onChange={(event) => {
          setKeyword(event.target.value)
          setPage(0)
        }}
        placeholder="Tìm theo tên, email..."
        aria-label="Tìm người dùng"
      />
      {list.isLoading ? (
        <Loading />
      ) : list.isError ? (
        <ErrorBox error={list.error} onRetry={list.refetch} />
      ) : list.data.items.length === 0 ? (
        <Empty title="Không có người dùng nào" />
      ) : (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Người dùng</th>
                  <th>Email</th>
                  <th>Tham gia</th>
                  <th>Quyền</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {list.data.items.map((user) => {
                  const isSelf = user.id === currentUser.id
                  return (
                    <tr key={user.id}>
                      <td>
                        <div className="table-user">
                          <Avatar url={user.avatarUrl} name={user.fullName} size={36} />
                          <span>
                            <strong>{user.fullName}</strong>
                            <small className="muted">@{user.username}</small>
                          </span>
                        </div>
                      </td>
                      <td>{user.email}</td>
                      <td>{formatDate(user.createdAt)}</td>
                      <td>
                        <select
                          value={user.roles.includes('ADMIN') ? 'ADMIN' : 'USER'}
                          disabled={isSelf}
                          aria-label={`Quyền của ${user.fullName}`}
                          onChange={(event) => {
                            if (window.confirm(`Đổi quyền của ${user.fullName} thành ${event.target.value}?`)) setRole.mutate({ id: user.id, role: event.target.value })
                          }}
                        >
                          <option value="USER">USER</option>
                          <option value="ADMIN">ADMIN</option>
                        </select>
                      </td>
                      <td>
                        <select
                          value={user.status}
                          disabled={isSelf}
                          aria-label={`Trạng thái của ${user.fullName}`}
                          onChange={(event) => {
                            if (window.confirm(`Đổi trạng thái của ${user.fullName} thành "${STATUS[event.target.value]}"?`)) setStatus.mutate({ id: user.id, status: event.target.value })
                          }}
                        >
                          {Object.entries(STATUS).map(([value, label]) => (
                            <option key={value} value={value}>
                              {label}
                            </option>
                          ))}
                        </select>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />
        </>
      )}
    </>
  )
}
