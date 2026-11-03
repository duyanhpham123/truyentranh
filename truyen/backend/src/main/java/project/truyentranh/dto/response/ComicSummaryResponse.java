package project.truyentranh.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import project.truyentranh.model.ComicStatus;

public record ComicSummaryResponse(Long id, String slug, String title, Long authorId, String authorName, String coverUrl, ComicStatus status, long views, BigDecimal ratingAvg, int ratingCount, BigDecimal latestChapter, Instant lastChapterAt, List<GenreResponse> genres) {

}
