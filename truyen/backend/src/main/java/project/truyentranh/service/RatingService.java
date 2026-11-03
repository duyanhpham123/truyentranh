package project.truyentranh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.RatingSummaryResponse;
import project.truyentranh.model.Comic;
import project.truyentranh.repository.ComicRepository;
import project.truyentranh.repository.RatingRepository;

@Service
@RequiredArgsConstructor
public class RatingService {

	private final RatingRepository ratings;

	private final ComicRepository comicRepository;

	private final ComicService comics;

	private final CacheService cache;

	@Transactional
	public RatingSummaryResponse rate(long userId, long comicId, int score) {
		String slug = comics.requireVisible(comicId).getSlug();
		ratings.upsert(userId, comicId, score);
		comicRepository.recalculateRating(comicId);
		cache.evictAfterCommit(ComicService.detailCacheKey(slug));
		return toSummary(comics.requireVisible(comicId));
	}

	@Transactional(readOnly = true)
	public RatingSummaryResponse summary(long comicId) {
		return toSummary(comics.requireVisible(comicId));
	}

	private RatingSummaryResponse toSummary(Comic comic) {
		return new RatingSummaryResponse(comic.getRatingAvg(), comic.getRatingCount());
	}

}
