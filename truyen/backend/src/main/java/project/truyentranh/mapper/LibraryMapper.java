package project.truyentranh.mapper;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.FavoriteResponse;
import project.truyentranh.dto.response.HistoryResponse;
import project.truyentranh.model.Comic;
import project.truyentranh.model.Favorite;
import project.truyentranh.model.ReadingHistory;
import project.truyentranh.service.StorageService;

/** Tủ truyện của user: lịch sử đọc và truyện đang theo dõi. */
@Component
@RequiredArgsConstructor
public class LibraryMapper {

	private final StorageService storage;

	public HistoryResponse toHistory(ReadingHistory h) {
		Comic c = h.getComic();
		return new HistoryResponse(c.getId(), c.getSlug(), c.getTitle(), storage.url(c.getCoverUrl()), h.getChapter().getId(),
				h.getChapter().getChapterNumber(), h.getLastPage(), h.getLastReadAt());
	}

	public FavoriteResponse toFavorite(Favorite f) {
		Comic c = f.getComic();
		return new FavoriteResponse(c.getId(), c.getSlug(), c.getTitle(), storage.url(c.getCoverUrl()), c.getLastChapterAt(),
				f.getCreatedAt());
	}

}
