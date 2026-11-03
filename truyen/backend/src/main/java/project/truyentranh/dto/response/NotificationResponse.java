package project.truyentranh.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record NotificationResponse(Long id, String type, String message, Long chapterId, BigDecimal chapterNumber, String comicSlug, Instant readAt, Instant createdAt) {

}
