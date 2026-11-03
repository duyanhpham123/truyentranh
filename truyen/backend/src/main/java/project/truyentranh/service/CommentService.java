package project.truyentranh.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.truyentranh.util.PageUtil;
import project.truyentranh.dto.request.CommentRequest;
import project.truyentranh.dto.response.CommentReportResponse;
import project.truyentranh.dto.response.CommentResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.CommentMapper;
import project.truyentranh.model.Comment;
import project.truyentranh.model.CommentReport;
import project.truyentranh.model.CommentStatus;
import project.truyentranh.model.ReportStatus;
import project.truyentranh.model.User;
import project.truyentranh.repository.ChapterRepository;
import project.truyentranh.repository.CommentReportRepository;
import project.truyentranh.repository.CommentRepository;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository comments;

	private final CommentReportRepository reports;

	private final ChapterRepository chapters;

	private final UserService users;

	private final ComicService comics;

	private final CommentMapper mapper;

	private final AuditService audit;

	@Transactional(readOnly = true)
	public PagedResponse<CommentResponse> list(long comicId, Long chapterId, int page, int size) {
		comics.requireVisible(comicId);
		Page<Comment> roots = comments.findRoots(comicId, chapterId, PageUtil.of(page, size));
		List<Long> rootIds = roots.getContent().stream().map(Comment::getId).toList();
		List<Comment> replies = rootIds.isEmpty() ? List.of() : comments.findReplies(rootIds);
		Map<Long, List<Comment>> repliesByRoot = replies.stream().collect(Collectors.groupingBy(Comment::getParentId));
		return PagedResponse.of(roots, root -> mapper.toResponse(root, repliesByRoot.getOrDefault(root.getId(), List.of())
			.stream()
			.map(r -> mapper.toResponse(r, List.of()))
			.toList()));
	}

	@Transactional
	public CommentResponse create(long userId, long comicId, CommentRequest request) {
		comics.requireVisible(comicId);
		User user = users.require(userId);
		if (user.getEmailVerifiedAt() == null) {
			throw ApiException.forbidden("EMAIL_NOT_VERIFIED", "Cần xác thực email trước khi bình luận");
		}
		Long chapterId = request.chapterId();
		if (chapterId != null) {
			chapters.findPublishedInComic(chapterId, comicId).orElseThrow(() -> ApiException.notFound("chapter"));
		}
		if (request.parentId() != null) {
			Comment parent = require(request.parentId());
			if (!parent.getComicId().equals(comicId) || parent.getParentId() != null) {
				throw ApiException.badRequest("INVALID_PARENT", "Chỉ có thể trả lời bình luận gốc của cùng truyện");
			}
			chapterId = parent.getChapterId();
		}
		Comment comment = new Comment();
		comment.setUser(user);
		comment.setComicId(comicId);
		comment.setChapterId(chapterId);
		comment.setParentId(request.parentId());
		comment.setContent(request.content().strip());
		comments.save(comment);
		return mapper.toResponse(comment, List.of());
	}

	@Transactional
	public void deleteOwn(long userId, long id) {
		Comment comment = require(id);
		if (!comment.getUser().getId().equals(userId)) {
			throw ApiException.forbidden("NOT_OWNER", "Chỉ được xóa bình luận của mình");
		}
		comment.setStatus(CommentStatus.DELETED);
	}

	@Transactional
	public void like(long userId, long id) {
		require(id);
		if (comments.addLike(id, userId) > 0) {
			comments.changeLikeCount(id, 1);
		}
	}

	@Transactional
	public void unlike(long userId, long id) {
		if (comments.removeLike(id, userId) > 0) {
			comments.changeLikeCount(id, -1);
		}
	}

	@Transactional
	public void report(long userId, long id, String reason) {
		Comment comment = require(id);
		if (reports.existsByCommentIdAndReporterId(id, userId)) {
			throw ApiException.conflict("ALREADY_REPORTED", "Bạn đã báo cáo bình luận này");
		}
		CommentReport report = new CommentReport();
		report.setComment(comment);
		report.setReporter(users.require(userId));
		report.setReason(reason.strip());
		reports.save(report);
	}

	// --- Admin ---

	@Transactional(readOnly = true)
	public PagedResponse<CommentReportResponse> reports(ReportStatus status, int page, int size) {
		return PagedResponse.of(reports.findByStatus(status, PageUtil.of(page, size)), mapper::toReport);
	}

	@Transactional
	public void resolveReport(long reportId, ReportStatus status) {
		CommentReport report = reports.findById(reportId).orElseThrow(() -> ApiException.notFound("báo cáo"));
		report.setStatus(status);
		audit.record("COMMENT_REPORT_RESOLVE", "COMMENT_REPORT", reportId, null, Map.of("status", status));
	}

	@Transactional
	public void adminHide(long id) {
		Comment comment = require(id);
		audit.record("COMMENT_HIDE", "COMMENT", id, Map.of("userId", comment.getUser().getId(), "content", comment.getContent()), null);
		comment.setStatus(CommentStatus.HIDDEN);
		reports.resolveAllOf(id);
	}

	private Comment require(long id) {
		return comments.findVisibleById(id).orElseThrow(() -> ApiException.notFound("bình luận"));
	}

}
