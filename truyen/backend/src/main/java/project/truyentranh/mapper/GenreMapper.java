package project.truyentranh.mapper;

import org.springframework.stereotype.Component;

import project.truyentranh.dto.response.GenreResponse;
import project.truyentranh.model.Genre;

@Component
public class GenreMapper {

	public GenreResponse toResponse(Genre g) {
		return new GenreResponse(g.getId(), g.getSlug(), g.getName(), g.getDescription());
	}

}
