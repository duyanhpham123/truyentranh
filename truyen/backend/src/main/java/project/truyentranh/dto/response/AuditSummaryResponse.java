package project.truyentranh.dto.response;

import java.time.Instant;

public record AuditSummaryResponse(Long id, Long actorId, String action, String targetType, Long targetId, Instant createdAt) {

}
