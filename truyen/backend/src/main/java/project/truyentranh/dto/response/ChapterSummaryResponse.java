package project.truyentranh.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ChapterSummaryResponse(Long id, BigDecimal chapterNumber, String chapterTitle, long views, Instant publishedAt) {

}
