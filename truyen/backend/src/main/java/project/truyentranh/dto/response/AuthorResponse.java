package project.truyentranh.dto.response;

import java.time.Instant;

public record AuthorResponse(Long id, String name, String biography, String avatarUrl, Instant createdAt) {

}
