package project.truyentranh.mapper;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.CommentAuthorResponse;
import project.truyentranh.dto.response.UserResponse;
import project.truyentranh.model.Role;
import project.truyentranh.model.User;
import project.truyentranh.service.StorageService;

@Component
@RequiredArgsConstructor
public class UserMapper {

	private final StorageService storage;

	public UserResponse toResponse(User u) {
		return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), storage.url(u.getAvatarUrl()),
				u.getRoles().stream().map(Role::getName).sorted().toList(), u.getStatus(), u.getEmailVerifiedAt() != null,
				u.getCreatedAt());
	}

	public CommentAuthorResponse toCommentAuthor(User u) {
		return new CommentAuthorResponse(u.getId(), u.getUsername(), u.getFullName(), storage.url(u.getAvatarUrl()));
	}

}
