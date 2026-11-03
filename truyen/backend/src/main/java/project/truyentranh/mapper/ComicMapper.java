package project.truyentranh.mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.ComicDetailResponse;
import project.truyentranh.dto.response.ComicSummaryResponse;
import project.truyentranh.dto.response.GenreResponse;
import project.truyentranh.model.Author;
import project.truyentranh.model.Comic;
import project.truyentranh.model.Genre;
import project.truyentranh.service.StorageService;

@Component
@RequiredArgsConstructor
public class ComicMapper {

	private final StorageService storage;

	private final GenreMapper genreMapper;

	public ComicSummaryResponse toSummary(Comic c, BigDecimal latestChapter) {
		Author a = c.getAuthor();
		return new ComicSummaryResponse(c.getId(), c.getSlug(), c.getTitle(), a == null ? null : a.getId(),
				a == null ? null : a.getName(), storage.url(c.getCoverUrl()), c.getStatus(), c.getViews(), c.getRatingAvg(),
				c.getRatingCount(), latestChapter, c.getLastChapterAt(), genres(c));
	}

	public ComicDetailResponse toDetail(Comic c) {
		Author a = c.getAuthor();
		return new ComicDetailResponse(c.getId(), c.getSlug(), c.getTitle(), a == null ? null : a.getId(),
				a == null ? null : a.getName(), c.getDescription(), storage.url(c.getCoverUrl()), c.getStatus(), c.isHidden(),
				c.getViews(), c.getRatingAvg(), c.getRatingCount(), c.getLastChapterAt(), c.getCreatedAt(), genres(c));
	}

	private List<GenreResponse> genres(Comic c) {
		return c.getGenres().stream().sorted(Comparator.comparing(Genre::getName)).map(genreMapper::toResponse).toList();
	}

}
