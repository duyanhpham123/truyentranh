package project.truyentranh.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.AdminChapterResponse;
import project.truyentranh.dto.response.ChapterPageResponse;
import project.truyentranh.dto.response.ChapterSummaryResponse;
import project.truyentranh.model.ChapterPage;
import project.truyentranh.model.ComicChapter;
import project.truyentranh.service.StorageService;

@Component
@RequiredArgsConstructor
public class ChapterMapper {

	private final StorageService storage;

	public ChapterSummaryResponse toSummary(ComicChapter c) {
		return new ChapterSummaryResponse(c.getId(), c.getChapterNumber(), c.getChapterTitle(), c.getViews(), c.getPublishedAt());
	}

	public ChapterPageResponse toPage(ChapterPage p) {
		return new ChapterPageResponse(p.getId(), p.getPageNumber(), storage.url(p.getImageUrl()), p.getWidth(), p.getHeight());
	}

	public AdminChapterResponse toAdmin(ComicChapter c, List<ChapterPageResponse> pages) {
		return new AdminChapterResponse(c.getId(), c.getComic().getId(), c.getChapterNumber(), c.getChapterTitle(), c.getStatus(),
				c.getViews(), c.getPublishedAt(), c.getCreatedAt(), pages);
	}

}
