package project.truyentranh.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import project.truyentranh.config.RabbitConfig;
import project.truyentranh.dto.request.ChapterRequest;
import project.truyentranh.dto.response.AdminChapterResponse;
import project.truyentranh.dto.response.ChapterPageResponse;
import project.truyentranh.dto.response.ChapterReadResponse;
import project.truyentranh.dto.response.ChapterSummaryResponse;
import project.truyentranh.event.OutboxPublisher;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.ChapterMapper;
import project.truyentranh.model.ChapterPage;
import project.truyentranh.model.ChapterStatus;
import project.truyentranh.model.Comic;
import project.truyentranh.model.ComicChapter;
import project.truyentranh.repository.ChapterPageRepository;
import project.truyentranh.repository.ChapterRepository;
import project.truyentranh.repository.ComicRepository;

@Service
@RequiredArgsConstructor
public class ChapterService {

	private final ChapterRepository chapters;

	private final ChapterPageRepository pages;

	private final ComicRepository comics;

	private final ComicService comicService;

	private final ChapterMapper mapper;

	private final StorageService storage;

	private final AuditService audit;

	private final OutboxPublisher outbox;

	private final TransactionTemplate tx;

	// --- Độc giả ---

	@Transactional(readOnly = true)
	public List<ChapterSummaryResponse> list(String comicSlug) {
		Comic comic = comicService.requireVisible(comicSlug);
		return chapters.findPublished(comic.getId()).stream().map(mapper::toSummary).toList();
	}

	@Transactional(readOnly = true)
	public ChapterReadResponse read(String comicSlug, BigDecimal number) {
		Comic comic = comicService.requireVisible(comicSlug);
		ComicChapter chapter = chapters.findPublishedByNumber(comic.getId(), number)
			.orElseThrow(() -> ApiException.notFound("chapter"));
		PageRequest first = PageRequest.of(0, 1);
		BigDecimal prev = chapters.findPreviousNumbers(comic.getId(), number, first).stream().findFirst().orElse(null);
		BigDecimal next = chapters.findNextNumbers(comic.getId(), number, first).stream().findFirst().orElse(null);
		return new ChapterReadResponse(chapter.getId(), comic.getId(), comic.getSlug(), comic.getTitle(), chapter.getChapterNumber(),
				chapter.getChapterTitle(), pageItems(chapter.getId()), prev, next);
	}

	// --- Admin ---

	@Transactional(readOnly = true)
	public List<AdminChapterResponse> adminList(long comicId) {
		requireComic(comicId);
		return chapters.findAllActive(comicId).stream().map(this::toAdmin).toList();
	}

	@Transactional
	public AdminChapterResponse create(long comicId, ChapterRequest request) {
		ComicChapter chapter = new ComicChapter();
		chapter.setComic(requireComic(comicId));
		chapter.setChapterNumber(request.chapterNumber());
		chapter.setChapterTitle(request.chapterTitle());
		chapters.saveAndFlush(chapter);
		AdminChapterResponse after = toAdmin(chapter);
		audit.record("CHAPTER_CREATE", "CHAPTER", chapter.getId(), null, after);
		return after;
	}

	@Transactional
	public AdminChapterResponse update(long id, ChapterRequest request) {
		ComicChapter chapter = require(id);
		AdminChapterResponse before = toAdmin(chapter);
		chapter.setChapterNumber(request.chapterNumber());
		chapter.setChapterTitle(request.chapterTitle());
		chapters.saveAndFlush(chapter);
		AdminChapterResponse after = toAdmin(chapter);
		audit.record("CHAPTER_UPDATE", "CHAPTER", id, before, after);
		return after;
	}

	@Transactional
	public void delete(long id) {
		ComicChapter chapter = require(id);
		audit.record("CHAPTER_DELETE", "CHAPTER", id, toAdmin(chapter), null);
		chapter.setDeletedAt(Instant.now());
		if (chapter.getStatus() == ChapterStatus.DRAFT) {
			// Bản nháp chưa ai đọc: bỏ luôn các trang, ảnh thành mồ côi và được job dọn
			pages.deleteByChapterId(id);
		}
		else {
			Comic comic = chapter.getComic();
			chapters.flush();
			comic.setLastChapterAt(chapters.findLastPublishedAt(comic.getId()));
			comicService.evict(comic);
		}
	}

	/** Upload ảnh nằm ngoài transaction để không giữ kết nối DB suốt lúc gửi file. */
	public AdminChapterResponse uploadPages(long id, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw ApiException.badRequest("NO_FILES", "Chưa chọn ảnh nào");
		}
		tx.executeWithoutResult(status -> require(id));
		List<StorageService.StoredImage> stored = new ArrayList<>();
		try {
			for (MultipartFile file : files) {
				stored.add(storage.store("chapters/" + id, file));
			}
			return tx.execute(status -> {
				ComicChapter chapter = require(id);
				int number = pages.findMaxPageNumber(id);
				for (StorageService.StoredImage image : stored) {
					ChapterPage page = new ChapterPage();
					page.setChapterId(id);
					page.setPageNumber(++number);
					page.setImageUrl(image.key());
					page.setWidth(image.width());
					page.setHeight(image.height());
					pages.save(page);
				}
				pages.flush();
				audit.record("CHAPTER_UPLOAD_PAGES", "CHAPTER", id, null, Map.of("added", stored.size()));
				return toAdmin(chapter);
			});
		}
		catch (RuntimeException ex) {
			stored.forEach(image -> storage.deleteQuietly(image.key()));
			throw ex;
		}
	}

	public AdminChapterResponse reorderPages(long id, List<Long> pageIds) {
		List<String> removedKeys = new ArrayList<>();
		AdminChapterResponse result = tx.execute(status -> {
			ComicChapter chapter = require(id);
			Map<Long, ChapterPage> current = pages.findByChapterIdOrderByPageNumberAsc(id).stream()
				.collect(Collectors.toMap(ChapterPage::getId, Function.identity()));
			if (new HashSet<>(pageIds).size() != pageIds.size() || !current.keySet().containsAll(pageIds)) {
				throw ApiException.badRequest("INVALID_PAGES", "Danh sách trang không hợp lệ");
			}
			List<Long> before = current.values().stream()
				.sorted(Comparator.comparingInt(ChapterPage::getPageNumber))
				.map(ChapterPage::getId)
				.toList();
			List<ChapterPage> removed = current.values().stream().filter(p -> !pageIds.contains(p.getId())).toList();
			removed.forEach(p -> removedKeys.add(p.getImageUrl()));
			pages.deleteAll(removed);
			pages.flush();
			// unique (chapter_id, page_number): đổi sang số âm trước để các dòng không đụng nhau khi đảo thứ tự
			pageIds.forEach(pageId -> current.get(pageId).setPageNumber(-pageId.intValue()));
			pages.flush();
			int number = 0;
			for (Long pageId : pageIds) {
				current.get(pageId).setPageNumber(++number);
			}
			pages.flush();
			audit.record("CHAPTER_REORDER_PAGES", "CHAPTER", id, Map.of("pageIds", before), Map.of("pageIds", pageIds));
			return toAdmin(chapter);
		});
		removedKeys.forEach(storage::deleteQuietly);
		return result;
	}

	/** Cập nhật chapter, truyện và ghi outbox trong cùng transaction nên không thể lệch nhau. */
	@Transactional
	public AdminChapterResponse publish(long id) {
		ComicChapter chapter = require(id);
		if (chapter.getStatus() == ChapterStatus.PUBLISHED) {
			throw ApiException.conflict("ALREADY_PUBLISHED", "Chapter đã được publish");
		}
		if (!pages.existsByChapterId(id)) {
			throw ApiException.badRequest("NO_PAGES", "Chapter phải có ít nhất 1 trang");
		}
		AdminChapterResponse before = toAdmin(chapter);
		Instant now = Instant.now();
		chapter.setStatus(ChapterStatus.PUBLISHED);
		chapter.setPublishedAt(now);
		Comic comic = chapter.getComic();
		comic.setLastChapterAt(now);
		outbox.add(RabbitConfig.CHAPTER_PUBLISHED, id);
		AdminChapterResponse after = toAdmin(chapter);
		audit.record("CHAPTER_PUBLISH", "CHAPTER", id, before, after);
		comicService.evict(comic);
		return after;
	}

	private ComicChapter require(long id) {
		return chapters.findActiveById(id).orElseThrow(() -> ApiException.notFound("chapter"));
	}

	private Comic requireComic(long id) {
		return comics.findActiveById(id).orElseThrow(() -> ApiException.notFound("truyện"));
	}

	private List<ChapterPageResponse> pageItems(long chapterId) {
		return pages.findByChapterIdOrderByPageNumberAsc(chapterId).stream().map(mapper::toPage).toList();
	}

	private AdminChapterResponse toAdmin(ComicChapter c) {
		return mapper.toAdmin(c, pageItems(c.getId()));
	}

}
