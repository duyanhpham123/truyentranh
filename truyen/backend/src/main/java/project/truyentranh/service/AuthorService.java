package project.truyentranh.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.AuthorRequest;
import project.truyentranh.dto.response.AuthorResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.AuthorMapper;
import project.truyentranh.model.Author;
import project.truyentranh.repository.AuthorRepository;
import project.truyentranh.repository.ComicRepository;

@Service
@RequiredArgsConstructor
public class AuthorService {

	private final AuthorRepository authors;

	private final ComicRepository comics;

	private final AuthorMapper mapper;

	private final AuditService audit;

	@Transactional(readOnly = true)
	public List<AuthorResponse> list() {
		return authors.findAllByOrderByNameAsc().stream().map(mapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public AuthorResponse detail(long id) {
		return mapper.toResponse(require(id));
	}

	@Transactional
	public AuthorResponse create(AuthorRequest request) {
		Author author = new Author();
		author.setName(request.name().strip());
		author.setBiography(request.biography());
		authors.save(author);
		AuthorResponse after = mapper.toResponse(author);
		audit.record("AUTHOR_CREATE", "AUTHOR", author.getId(), null, after);
		return after;
	}

	@Transactional
	public AuthorResponse update(long id, AuthorRequest request) {
		Author author = require(id);
		AuthorResponse before = mapper.toResponse(author);
		author.setName(request.name().strip());
		author.setBiography(request.biography());
		AuthorResponse after = mapper.toResponse(author);
		audit.record("AUTHOR_UPDATE", "AUTHOR", id, before, after);
		return after;
	}

	@Transactional
	public void delete(long id) {
		Author author = require(id);
		if (comics.existsByAuthorId(id)) {
			throw ApiException.conflict("AUTHOR_IN_USE", "Tác giả còn truyện đang gắn, không thể xóa");
		}
		audit.record("AUTHOR_DELETE", "AUTHOR", id, mapper.toResponse(author), null);
		authors.delete(author);
	}

	private Author require(long id) {
		return authors.findById(id).orElseThrow(() -> ApiException.notFound("tác giả"));
	}

}
