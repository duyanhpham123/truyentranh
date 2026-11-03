package project.truyentranh.dto.response;

import java.time.Instant;
import java.util.List;
import project.truyentranh.model.UserStatus;

public record UserResponse(Long id, String username, String email, String fullName, String avatarUrl, List<String> roles, UserStatus status, boolean emailVerified, Instant createdAt) {

}
