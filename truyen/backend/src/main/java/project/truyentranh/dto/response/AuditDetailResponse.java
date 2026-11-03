package project.truyentranh.dto.response;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonRawValue;

public record AuditDetailResponse(Long id, Long actorId, String action, String targetType, Long targetId, @JsonRawValue String beforeData, @JsonRawValue String afterData, Instant createdAt) {

}
