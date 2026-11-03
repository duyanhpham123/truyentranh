package project.truyentranh.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import project.truyentranh.model.ComicStatus;

public record ComicDetailResponse(Long id, String slug, String title, Long authorId, String authorName, String description, String coverUrl, ComicStatus status, boolean hidden, long views, BigDecimal ratingAvg, int ratingCount, Instant lastChapterAt, Instant createdAt, List<GenreResponse> genres) {

}
