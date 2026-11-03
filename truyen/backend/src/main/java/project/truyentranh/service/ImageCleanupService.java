package project.truyentranh.service;

import java.time.Duration;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.truyentranh.repository.ChapterPageRepository;
import project.truyentranh.repository.ComicRepository;
import project.truyentranh.repository.UserRepository;

/** Dọn ảnh không còn bản ghi nào trỏ tới; chừa 1 ngày để không đụng file đang upload dở. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCleanupService {

	private static final Duration MIN_AGE = Duration.ofDays(1);

	private final StorageService storage;

	private final ChapterPageRepository pages;

	private final ComicRepository comics;

	private final UserRepository users;

	@Scheduled(cron = "0 0 4 * * *")
	public void cleanOrphanImages() {
		int deleted = storage.deleteOrphans("chapters/", MIN_AGE, pages::existsByImageUrl)
				+ storage.deleteOrphans("covers/", MIN_AGE, comics::existsByCoverUrl)
				+ storage.deleteOrphans("avatars/", MIN_AGE, users::existsByAvatarUrl);
		log.info("Đã dọn {} ảnh mồ côi", deleted);
	}

}
