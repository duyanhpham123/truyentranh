package project.truyentranh.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ChapterReadResponse(Long id, Long comicId, String comicSlug, String comicTitle, BigDecimal chapterNumber, String chapterTitle, List<ChapterPageResponse> pages, BigDecimal prevChapter, BigDecimal nextChapter) {

}
