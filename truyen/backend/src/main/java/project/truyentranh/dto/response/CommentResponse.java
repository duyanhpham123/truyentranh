package project.truyentranh.dto.response;

import java.time.Instant;
import java.util.List;

public record CommentResponse(Long id, Long comicId, Long chapterId, Long parentId, CommentAuthorResponse author, String content, int likeCount, Instant createdAt, List<CommentResponse> replies) {

}
