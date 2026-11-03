package project.truyentranh.mapper;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.AuthorResponse;
import project.truyentranh.model.Author;
import project.truyentranh.service.StorageService;

@Component
@RequiredArgsConstructor
public class AuthorMapper {

	private final StorageService storage;

	public AuthorResponse toResponse(Author a) {
		return new AuthorResponse(a.getId(), a.getName(), a.getBiography(), storage.url(a.getAvatarUrl()), a.getCreatedAt());
	}

}
