package project.truyentranh.dto.response;

import java.time.Instant;
import project.truyentranh.model.ReportStatus;

public record CommentReportResponse(Long id, Long commentId, String commentContent, Long commentAuthorId, Long reporterId, String reporterName, String reason, ReportStatus status, Instant createdAt) {

}
