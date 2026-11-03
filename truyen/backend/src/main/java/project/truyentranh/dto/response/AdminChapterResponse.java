package project.truyentranh.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import project.truyentranh.model.ChapterStatus;

public record AdminChapterResponse(Long id, Long comicId, BigDecimal chapterNumber, String chapterTitle, ChapterStatus status, long views, Instant publishedAt, Instant createdAt, List<ChapterPageResponse> pages) {

}
