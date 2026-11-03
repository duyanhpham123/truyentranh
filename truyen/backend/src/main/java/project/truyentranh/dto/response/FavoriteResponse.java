package project.truyentranh.dto.response;

import java.time.Instant;

public record FavoriteResponse(Long comicId, String comicSlug, String comicTitle, String coverUrl, Instant lastChapterAt, Instant followedAt) {

}
