package project.truyentranh.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.truyentranh.exception.ApiException;
import project.truyentranh.model.ComicChapter;
import project.truyentranh.repository.ChapterRepository;
import project.truyentranh.repository.ViewStatRepository;
import project.truyentranh.util.TimeUtil;

/** Đếm view trên Redis rồi gom ghi xuống MySQL theo lô, tránh mỗi lượt đọc một câu UPDATE. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewService {

	private static final Duration DEDUPE_TTL = Duration.ofHours(1);

	private static final String PENDING_COMIC = "views:pending:comic";

	private static final String PENDING_CHAPTER = "views:pending:chapter";

	private static final String FLUSHING_SUFFIX = ":flushing";

	private final StringRedisTemplate redis;

	private final ChapterRepository chapters;

	private final ViewStatRepository stats;

	private final TransactionTemplate tx;

	@Transactional(readOnly = true)
	public void record(long chapterId, String viewer) {
		ComicChapter chapter = chapters.findReadable(chapterId).orElseThrow(() -> ApiException.notFound("chapter"));
		long comicId = chapter.getComic().getId();
		try {
			String seenKey = "views:seen:" + chapterId + ":" + viewer;
			if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(seenKey, "1", DEDUPE_TTL))) {
				return;
			}
			redis.opsForHash().increment(PENDING_COMIC, String.valueOf(comicId), 1);
			redis.opsForHash().increment(PENDING_CHAPTER, String.valueOf(chapterId), 1);
			String dayKey = dayKey(TimeUtil.today());
			redis.opsForZSet().incrementScore(dayKey, String.valueOf(comicId), 1);
			redis.expire(dayKey, Duration.ofDays(32));
		}
		catch (DataAccessException ex) {
			// Mất vài lượt view còn hơn làm lỗi trang đọc truyện
			log.warn("Không ghi nhận được view: {}", ex.getMessage());
		}
	}

	/** Id truyện xem nhiều nhất trong kỳ, cộng dồn từ các ZSET theo ngày. */
	public List<Long> hotComicIds(String period, int limit) {
		int days = switch (period) {
			case "day" -> 1;
			case "week" -> 7;
			default -> 30;
		};
		LocalDate today = TimeUtil.today();
		List<String> keys = new ArrayList<>();
		for (int i = 0; i < days; i++) {
			keys.add(dayKey(today.minusDays(i)));
		}
		try {
			String destination = "views:hot:" + period;
			redis.opsForZSet().unionAndStore(keys.get(0), keys.subList(1, keys.size()), destination);
			redis.expire(destination, Duration.ofMinutes(5));
			Set<String> ids = redis.opsForZSet().reverseRange(destination, 0, limit - 1L);
			return ids == null ? List.of() : ids.stream().map(Long::valueOf).toList();
		}
		catch (DataAccessException ex) {
			log.warn("Không đọc được bảng xếp hạng hot: {}", ex.getMessage());
			return List.of();
		}
	}

	@Scheduled(fixedDelayString = "${app.views.flush-delay:PT5M}", initialDelayString = "${app.views.flush-delay:PT5M}")
	public void flush() {
		try {
			LocalDate today = TimeUtil.today();
			flushHash(PENDING_COMIC, (id, count) -> stats.addComicViews(id, count, today));
			flushHash(PENDING_CHAPTER, stats::addChapterViews);
		}
		catch (DataAccessException ex) {
			log.warn("Gom view thất bại, sẽ thử lại lần sau: {}", ex.getMessage());
		}
	}

	private interface Applier {

		void apply(long id, long count);

	}

	/**
	 * Đổi tên hash sang khóa tạm rồi mới đọc, nên view đến trong lúc gom không bị mất. Khóa tạm chỉ bị xóa sau khi
	 * transaction thành công; lần chạy sau sẽ xử lý tiếp nếu job dừng giữa chừng.
	 */
	private void flushHash(String key, Applier applier) {
		String flushing = key + FLUSHING_SUFFIX;
		if (!Boolean.TRUE.equals(redis.hasKey(flushing))) {
			if (!Boolean.TRUE.equals(redis.hasKey(key))) {
				return;
			}
			redis.rename(key, flushing);
		}
		Map<Object, Object> entries = redis.opsForHash().entries(flushing);
		tx.executeWithoutResult(status -> entries.forEach((id, count) -> applier
			.apply(Long.parseLong(Objects.toString(id)), Long.parseLong(Objects.toString(count)))));
		redis.delete(flushing);
	}

	private String dayKey(LocalDate date) {
		return "views:day:" + date;
	}

}
