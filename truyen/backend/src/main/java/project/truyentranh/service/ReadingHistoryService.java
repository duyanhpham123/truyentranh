package project.truyentranh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.truyentranh.util.PageUtil;
import project.truyentranh.dto.response.HistoryResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.LibraryMapper;
import project.truyentranh.repository.ChapterRepository;
import project.truyentranh.repository.ReadingHistoryRepository;

@Service
@RequiredArgsConstructor
public class ReadingHistoryService {

	private final ReadingHistoryRepository histories;

	private final ChapterRepository chapters;

	private final ComicService comics;

	private final LibraryMapper mapper;

	@Transactional
	public void saveProgress(long userId, long comicId, long chapterId, int page) {
		comics.requireVisible(comicId);
		chapters.findPublishedInComic(chapterId, comicId).orElseThrow(() -> ApiException.notFound("chapter"));
		histories.upsert(userId, comicId, chapterId, page);
	}

	@Transactional(readOnly = true)
	public PagedResponse<HistoryResponse> list(long userId, int page, int size) {
		return PagedResponse.of(histories.findVisible(userId, PageUtil.of(page, size)), mapper::toHistory);
	}

	@Transactional
	public void delete(long userId, long comicId) {
		histories.remove(userId, comicId);
	}

}
