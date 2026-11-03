import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { comics, comments as commentsApi } from '../api/index.js'
import { errorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { timeAgo } from '../lib/format.js'
import { useToast } from './Toast.jsx'
import { Avatar, Empty, Loading, LoginPrompt, Pagination } from './Ui.jsx'
import Icon from './Icon.jsx'

function CommentForm({ comicId, chapterId, parentId, onDone, placeholder = 'Bạn nghĩ gì về truyện này?' }) {
  const [content, setContent] = useState('')
  const toast = useToast()
  const queryClient = useQueryClient()
  const send = useMutation({
    mutationFn: () => comics.comment(comicId, { content, chapterId, parentId }),
    onSuccess: () => {
      setContent('')
      queryClient.invalidateQueries({ queryKey: ['comments', comicId] })
      onDone?.()
    },
    onError: (error) => toast.error(errorMessage(error)),
  })
  return (
    <form
      className="comment-form"
      onSubmit={(event) => {
        event.preventDefault()
        if (content.trim()) send.mutate()
      }}
    >
      <textarea value={content} onChange={(event) => setContent(event.target.value)} placeholder={placeholder} maxLength={2000} rows={parentId ? 2 : 3} aria-label="Nội dung bình luận" />
      <button className="btn btn-primary btn-small" disabled={send.isPending || !content.trim()}>
        {send.isPending ? 'Đang gửi...' : 'Gửi'}
      </button>
    </form>
  )
}

function CommentItem({ comment, comicId, chapterId, isReply = false }) {
  const { user } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [replying, setReplying] = useState(false)
  // API không trả về "đã like hay chưa", nên chỉ nhớ trong phiên xem hiện tại
  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(comment.likeCount)

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['comments', comicId] })
  const onError = (error) => toast.error(errorMessage(error))

  const like = useMutation({
    mutationFn: () => (liked ? commentsApi.unlike(comment.id) : commentsApi.like(comment.id)),
    onSuccess: () => {
      setLikeCount((count) => count + (liked ? -1 : 1))
      setLiked(!liked)
    },
    onError,
  })
  const remove = useMutation({ mutationFn: () => commentsApi.remove(comment.id), onSuccess: refresh, onError })
  const report = useMutation({
    mutationFn: (reason) => commentsApi.report(comment.id, reason),
    onSuccess: () => toast.success('Đã gửi báo cáo, cảm ơn bạn.'),
    onError,
  })

  return (
    <li className={`comment ${isReply ? 'comment-reply' : ''}`}>
      <Avatar url={comment.author.avatarUrl} name={comment.author.fullName} size={isReply ? 34 : 42} />
      <div className="comment-main">
        <div className="comment-bubble">
          <div className="comment-head">
            <strong>{comment.author.fullName}</strong>
            <time dateTime={comment.createdAt}>{timeAgo(comment.createdAt)}</time>
          </div>
          <p>{comment.content}</p>
        </div>
        <div className="comment-actions">
          <button disabled={!user || like.isPending} onClick={() => like.mutate()} className={liked ? 'is-active' : ''} aria-pressed={liked}>
            <Icon name="heart" size={15} filled={liked} /> {likeCount}
          </button>
          {user && !isReply && (
            <button onClick={() => setReplying((value) => !value)}>
              <Icon name="reply" size={15} /> Trả lời
            </button>
          )}
          {user && user.id !== comment.author.id && (
            <button
              onClick={() => {
                const reason = window.prompt('Lý do báo cáo bình luận này?')
                if (reason?.trim()) report.mutate(reason.trim())
              }}
            >
              <Icon name="flag" size={15} /> Báo cáo
            </button>
          )}
          {user?.id === comment.author.id && (
            <button
              onClick={() => {
                if (window.confirm('Xóa bình luận này?')) remove.mutate()
              }}
            >
              <Icon name="trash" size={15} /> Xóa
            </button>
          )}
        </div>
        {replying && (
          <CommentForm comicId={comicId} chapterId={chapterId} parentId={comment.id} placeholder={`Trả lời ${comment.author.fullName}...`} onDone={() => setReplying(false)} />
        )}
        {comment.replies?.length > 0 && (
          <ul className="comment-list">
            {comment.replies.map((reply) => (
              <CommentItem key={reply.id} comment={reply} comicId={comicId} chapterId={chapterId} isReply />
            ))}
          </ul>
        )}
      </div>
    </li>
  )
}

export default function Comments({ comicId, chapterId }) {
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const { data, isLoading } = useQuery({
    queryKey: ['comments', comicId, chapterId, page],
    queryFn: () => comics.comments(comicId, { chapterId, page, size: 10 }),
  })

  return (
    <section className="comments">
      <h2 className="comments-title">
        Bình luận {data ? <span className="count-pill">{data.totalItems}</span> : null}
      </h2>
      {user ? (
        user.emailVerified ? (
          <CommentForm comicId={comicId} chapterId={chapterId} />
        ) : (
          <p className="login-prompt">Hãy xác thực email trong trang tài khoản để bình luận.</p>
        )
      ) : (
        <LoginPrompt>để bình luận</LoginPrompt>
      )}
      {isLoading ? (
        <Loading />
      ) : data?.items.length ? (
        <>
          <ul className="comment-list">
            {data.items.map((comment) => (
              <CommentItem key={comment.id} comment={comment} comicId={comicId} chapterId={chapterId} />
            ))}
          </ul>
          <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
        </>
      ) : (
        <Empty title="Chưa có bình luận nào">Hãy là người đầu tiên lên tiếng!</Empty>
      )}
    </section>
  )
}
