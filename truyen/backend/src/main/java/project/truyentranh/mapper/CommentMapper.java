package project.truyentranh.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.CommentReportResponse;
import project.truyentranh.dto.response.CommentResponse;
import project.truyentranh.model.Comment;
import project.truyentranh.model.CommentReport;

@Component
@RequiredArgsConstructor
public class CommentMapper {

	private final UserMapper userMapper;

	public CommentResponse toResponse(Comment c, List<CommentResponse> replies) {
		return new CommentResponse(c.getId(), c.getComicId(), c.getChapterId(), c.getParentId(),
				userMapper.toCommentAuthor(c.getUser()), c.getContent(), c.getLikeCount(), c.getCreatedAt(), replies);
	}

	public CommentReportResponse toReport(CommentReport r) {
		Comment c = r.getComment();
		return new CommentReportResponse(r.getId(), c.getId(), c.getContent(), c.getUser().getId(), r.getReporter().getId(),
				r.getReporter().getFullName(), r.getReason(), r.getStatus(), r.getCreatedAt());
	}

}
