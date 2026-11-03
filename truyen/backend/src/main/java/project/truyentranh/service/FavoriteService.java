package project.truyentranh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.truyentranh.util.PageUtil;
import project.truyentranh.dto.response.FavoriteResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.mapper.LibraryMapper;
import project.truyentranh.repository.FavoriteRepository;

@Service
@RequiredArgsConstructor
public class FavoriteService {

	private final FavoriteRepository favorites;

	private final ComicService comics;

	private final LibraryMapper mapper;

	@Transactional
	public void follow(long userId, long comicId) {
		comics.requireVisible(comicId);
		favorites.addIfAbsent(userId, comicId);
	}

	@Transactional
	public void unfollow(long userId, long comicId) {
		favorites.remove(userId, comicId);
	}

	@Transactional(readOnly = true)
	public PagedResponse<FavoriteResponse> list(long userId, int page, int size) {
		return PagedResponse.of(favorites.findVisible(userId, PageUtil.of(page, size)), mapper::toFavorite);
	}

}
