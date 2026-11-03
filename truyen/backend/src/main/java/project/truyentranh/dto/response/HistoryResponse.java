package project.truyentranh.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoryResponse(Long comicId, String comicSlug, String comicTitle, String coverUrl, Long chapterId, BigDecimal chapterNumber, int lastPage, Instant lastReadAt) {

}
