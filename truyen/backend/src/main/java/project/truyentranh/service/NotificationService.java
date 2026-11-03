package project.truyentranh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.truyentranh.util.PageUtil;
import project.truyentranh.dto.response.NotificationResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.NotificationMapper;
import project.truyentranh.model.ComicChapter;
import project.truyentranh.repository.ChapterRepository;
import project.truyentranh.repository.NotificationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	private static final String NEW_CHAPTER = "NEW_CHAPTER";

	private final NotificationRepository notifications;

	private final ChapterRepository chapters;

	private final NotificationMapper mapper;

	@Transactional
	public void createForNewChapter(long chapterId) {
		ComicChapter chapter = chapters.findReadable(chapterId).orElse(null);
		if (chapter == null) {
			log.info("Bỏ qua thông báo vì chapter {} không còn hiển thị", chapterId);
			return;
		}
		String message = chapter.getComic().getTitle() + " đã có chapter "
				+ chapter.getChapterNumber().stripTrailingZeros().toPlainString();
		int created = notifications.createForFollowers(chapterId, NEW_CHAPTER, message, chapter.getComic().getId());
		log.info("Đã tạo {} thông báo cho chapter {}", created, chapterId);
	}

	@Transactional(readOnly = true)
	public PagedResponse<NotificationResponse> list(long userId, int page, int size) {
		return PagedResponse.of(notifications.findForUser(userId, PageUtil.of(page, size)), mapper::toResponse);
	}

	@Transactional(readOnly = true)
	public long unreadCount(long userId) {
		return notifications.countByUserIdAndReadAtIsNull(userId);
	}

	@Transactional
	public void markRead(long userId, long id) {
		if (!notifications.existsByIdAndUserId(id, userId)) {
			throw ApiException.notFound("thông báo");
		}
		notifications.markRead(id);
	}

	@Transactional
	public void markAllRead(long userId) {
		notifications.markAllRead(userId);
	}

}
