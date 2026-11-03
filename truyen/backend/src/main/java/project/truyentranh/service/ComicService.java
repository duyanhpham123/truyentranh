package project.truyentranh.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import project.truyentranh.util.PageUtil;
import project.truyentranh.dto.request.ComicRequest;
import project.truyentranh.dto.response.ComicDetailResponse;
import project.truyentranh.dto.response.ComicSummaryResponse;
import project.truyentranh.dto.response.LastReadResponse;
import project.truyentranh.dto.response.MyComicStateResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.ComicMapper;
import project.truyentranh.model.Comic;
import project.truyentranh.model.ComicStatus;
import project.truyentranh.model.Genre;
import project.truyentranh.repository.AuthorRepository;
import project.truyentranh.repository.ChapterRepository;
import project.truyentranh.repository.ComicRepository;
import project.truyentranh.repository.FavoriteRepository;
import project.truyentranh.repository.GenreRepository;
import project.truyentranh.repository.RatingRepository;
import project.truyentranh.repository.ReadingHistoryRepository;
import project.truyentranh.util.SlugUtil;
import tools.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
public class ComicService {

	private static final String LATEST_CACHE_KEY = "cache:comics:latest";

	private static final int HOME_PAGE_SIZE = 24;

	private static final int HOT_LIMIT = 20;

	private static final Map<String, Sort> SORTS = Map.of(
			"latest", Sort.by(Sort.Direction.DESC, "lastChapterAt", "id"),
			"views", Sort.by(Sort.Direction.DESC, "views", "id"),
			"rating", Sort.by(Sort.Direction.DESC, "ratingAvg", "ratingCount", "id"));

	private final ComicRepository comics;

	private final GenreRepository genres;

	private final AuthorRepository authors;

	private final ChapterRepository chapters;

	private final FavoriteRepository favorites;

	private final RatingRepository ratings;

	private final ReadingHistoryRepository histories;

	private final ComicMapper mapper;

	private final StorageService storage;

	private final CacheService cache;

	private final AuditService audit;

	private final ViewService views;

	private final TransactionTemplate tx;

	public static String detailCacheKey(String slug) {
		return "cache:comic:" + slug;
	}

	private static String hotCacheKey(String period) {
		return "cache:comics:hot:" + period;
	}

	@Transactional(readOnly = true)
	public PagedResponse<ComicSummaryResponse> search(String keyword, String genre, ComicStatus status, Long authorId, String sort,
			int page, int size) {
		Sort order = SORTS.get(sort);
		if (order == null) {
			throw ApiException.badRequest("INVALID_SORT", "sort chỉ nhận latest, views hoặc rating");
		}
		boolean isHomePage = !StringUtils.hasText(keyword) && !StringUtils.hasText(genre) && status == null && authorId == null
				&& sort.equals("latest") && page == 0 && size == HOME_PAGE_SIZE;
		if (isHomePage) {
			return cache.getOrLoad(LATEST_CACHE_KEY, Duration.ofMinutes(5), new TypeReference<>() {
			}, () -> runSearch(null, null, null, null, order, page, size));
		}
		return runSearch(keyword, genre, status, authorId, order, page, size);
	}

	private PagedResponse<ComicSummaryResponse> runSearch(String keyword, String genre, ComicStatus status, Long authorId, Sort order,
			int page, int size) {
		Page<Comic> result = comics.search(likeParam(keyword), StringUtils.hasText(genre) ? genre : null, status, authorId,
				PageUtil.of(page, size, order));
		Map<Long, BigDecimal> latest = latestChapters(result.getContent().stream().map(Comic::getId).toList());
		return PagedResponse.of(result, c -> mapper.toSummary(c, latest.get(c.getId())));
	}

	@Transactional(readOnly = true)
	public ComicDetailResponse detail(String slug) {
		return cache.getOrLoad(detailCacheKey(slug), Duration.ofMinutes(10), new TypeReference<>() {
		}, () -> mapper.toDetail(requireVisible(slug)));
	}

	@Transactional(readOnly = true)
	public List<ComicSummaryResponse> hot(String period) {
		if (!List.of("day", "week", "month").contains(period)) {
			throw ApiException.badRequest("INVALID_PERIOD", "period chỉ nhận day, week hoặc month");
		}
		return cache.getOrLoad(hotCacheKey(period), Duration.ofMinutes(1), new TypeReference<>() {
		}, () -> loadHot(period));
	}

	private List<ComicSummaryResponse> loadHot(String period) {
		List<Long> ids = views.hotComicIds(period, HOT_LIMIT);
		Map<Long, Integer> rank = new HashMap<>();
		ids.forEach(id -> rank.put(id, rank.size()));
		List<Comic> found = new ArrayList<>(comics.findVisibleByIds(ids.isEmpty() ? List.of(-1L) : ids));
		found.sort(Comparator.comparing(c -> rank.get(c.getId())));
		// Trong kỳ ít truyện có lượt xem (site mới, Redis trống): bù bằng truyện có tổng view cao nhất
		for (Comic comic : comics.findMostViewed(PageRequest.of(0, HOT_LIMIT))) {
			if (found.size() < HOT_LIMIT && !rank.containsKey(comic.getId())) {
				found.add(comic);
			}
		}
		Map<Long, BigDecimal> latest = latestChapters(found.stream().map(Comic::getId).toList());
		return found.stream().map(c -> mapper.toSummary(c, latest.get(c.getId()))).toList();
	}

	@Transactional(readOnly = true)
	public MyComicStateResponse myState(long userId, long comicId) {
		requireVisible(comicId);
		Integer rating = ratings.findByUserIdAndComicId(userId, comicId).map(r -> r.getScore().intValue()).orElse(null);
		LastReadResponse lastRead = histories.findOne(userId, comicId)
			.map(h -> new LastReadResponse(h.getChapter().getId(), h.getChapter().getChapterNumber(), h.getLastPage()))
			.orElse(null);
		return new MyComicStateResponse(favorites.existsByUserIdAndComicId(userId, comicId), rating, lastRead);
	}

	public Comic requireVisible(long id) {
		return comics.findVisibleById(id).orElseThrow(() -> ApiException.notFound("truyện"));
	}

	public Comic requireVisible(String slug) {
		return comics.findVisibleBySlug(slug).orElseThrow(() -> ApiException.notFound("truyện"));
	}

	// --- Admin ---

	@Transactional(readOnly = true)
	public PagedResponse<ComicDetailResponse> adminList(String keyword, int page, int size) {
		return PagedResponse.of(comics.searchForAdmin(likeParam(keyword), PageUtil.of(page, size, Sort.by(Sort.Direction.DESC, "id"))),
				mapper::toDetail);
	}

	@Transactional
	public ComicDetailResponse create(ComicRequest request) {
		Comic comic = new Comic();
		comic.setSlug(uniqueSlug(request.title()));
		apply(comic, request);
		comics.save(comic);
		ComicDetailResponse after = mapper.toDetail(comic);
		audit.record("COMIC_CREATE", "COMIC", comic.getId(), null, after);
		evict(comic);
		return after;
	}

	@Transactional
	public ComicDetailResponse update(long id, ComicRequest request) {
		Comic comic = requireActive(id);
		ComicDetailResponse before = mapper.toDetail(comic);
		apply(comic, request);
		ComicDetailResponse after = mapper.toDetail(comic);
		audit.record("COMIC_UPDATE", "COMIC", id, before, after);
		evict(comic);
		return after;
	}

	@Transactional
	public void delete(long id) {
		Comic comic = requireActive(id);
		audit.record("COMIC_DELETE", "COMIC", id, mapper.toDetail(comic), null);
		comic.setDeletedAt(Instant.now());
		evict(comic);
	}

	@Transactional
	public ComicDetailResponse setHidden(long id, boolean hidden) {
		Comic comic = requireActive(id);
		ComicDetailResponse before = mapper.toDetail(comic);
		comic.setHidden(hidden);
		ComicDetailResponse after = mapper.toDetail(comic);
		audit.record("COMIC_VISIBILITY", "COMIC", id, before, after);
		evict(comic);
		return after;
	}

	/** Upload nằm ngoài transaction để không giữ kết nối DB khi đang gửi file. */
	public ComicDetailResponse changeCover(long id, MultipartFile file) {
		String key = storage.store("covers/" + id, file).key();
		String[] oldKey = new String[1];
		ComicDetailResponse after = tx.execute(status -> {
			Comic comic = requireActive(id);
			ComicDetailResponse before = mapper.toDetail(comic);
			oldKey[0] = comic.getCoverUrl();
			comic.setCoverUrl(key);
			ComicDetailResponse updated = mapper.toDetail(comic);
			audit.record("COMIC_COVER", "COMIC", id, before, updated);
			evict(comic);
			return updated;
		});
		storage.deleteQuietly(oldKey[0]);
		return after;
	}

	public void evict(Comic comic) {
		cache.evictAfterCommit(LATEST_CACHE_KEY, detailCacheKey(comic.getSlug()), hotCacheKey("day"), hotCacheKey("week"), hotCacheKey("month"));
	}

	private Comic requireActive(long id) {
		return comics.findActiveById(id).orElseThrow(() -> ApiException.notFound("truyện"));
	}

	private void apply(Comic comic, ComicRequest request) {
		comic.setTitle(request.title().strip());
		comic.setDescription(request.description());
		comic.setStatus(request.status());
		comic.setAuthor(request.authorId() == null ? null : authors.findById(request.authorId())
			.orElseThrow(() -> ApiException.badRequest("INVALID_AUTHOR", "Tác giả không tồn tại")));
		Collection<Long> ids = request.genreIds() == null ? List.of() : request.genreIds();
		List<Genre> found = genres.findAllById(ids);
		if (found.size() != ids.size()) {
			throw ApiException.badRequest("INVALID_GENRE", "Có thể loại không tồn tại");
		}
		comic.setGenres(new HashSet<>(found));
	}

	private String uniqueSlug(String title) {
		String base = SlugUtil.toSlug(title);
		String slug = base;
		for (int i = 2; comics.existsBySlug(slug); i++) {
			slug = base + "-" + i;
		}
		return slug;
	}

	private String likeParam(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return null;
		}
		return keyword.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private Map<Long, BigDecimal> latestChapters(List<Long> comicIds) {
		if (comicIds.isEmpty()) {
			return Map.of();
		}
		return chapters.findLatestNumbers(comicIds).stream()
			.collect(Collectors.toMap(ChapterRepository.LatestNumber::getComicId, ChapterRepository.LatestNumber::getNumber));
	}

}
