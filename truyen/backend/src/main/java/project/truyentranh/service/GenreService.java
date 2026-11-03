package project.truyentranh.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.GenreRequest;
import project.truyentranh.dto.response.GenreResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.GenreMapper;
import project.truyentranh.model.Genre;
import project.truyentranh.repository.GenreRepository;
import project.truyentranh.util.SlugUtil;

@Service
@RequiredArgsConstructor
public class GenreService {

	private final GenreRepository genres;

	private final GenreMapper mapper;

	private final AuditService audit;

	@Transactional(readOnly = true)
	public List<GenreResponse> list() {
		return genres.findAllByOrderByNameAsc().stream().map(mapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public GenreResponse detail(String slug) {
		return genres.findBySlug(slug).map(mapper::toResponse).orElseThrow(() -> ApiException.notFound("thể loại"));
	}

	@Transactional
	public GenreResponse create(GenreRequest request) {
		String slug = SlugUtil.toSlug(request.name());
		if (genres.existsBySlug(slug)) {
			throw ApiException.conflict("GENRE_EXISTS", "Thể loại đã tồn tại");
		}
		Genre genre = new Genre();
		genre.setSlug(slug);
		genre.setName(request.name().strip());
		genre.setDescription(request.description());
		genres.save(genre);
		GenreResponse after = mapper.toResponse(genre);
		audit.record("GENRE_CREATE", "GENRE", genre.getId(), null, after);
		return after;
	}

	@Transactional
	public GenreResponse update(long id, GenreRequest request) {
		Genre genre = require(id);
		GenreResponse before = mapper.toResponse(genre);
		genre.setName(request.name().strip());
		genre.setDescription(request.description());
		GenreResponse after = mapper.toResponse(genre);
		audit.record("GENRE_UPDATE", "GENRE", id, before, after);
		return after;
	}

	@Transactional
	public void delete(long id) {
		Genre genre = require(id);
		if (genres.countComics(id) > 0) {
			throw ApiException.conflict("GENRE_IN_USE", "Thể loại còn truyện đang gắn, không thể xóa");
		}
		audit.record("GENRE_DELETE", "GENRE", id, mapper.toResponse(genre), null);
		genres.delete(genre);
	}

	private Genre require(long id) {
		return genres.findById(id).orElseThrow(() -> ApiException.notFound("thể loại"));
	}

}
