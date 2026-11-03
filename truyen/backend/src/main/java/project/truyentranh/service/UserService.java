package project.truyentranh.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import project.truyentranh.util.PageUtil;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.dto.response.UserResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.UserMapper;
import project.truyentranh.model.Role;
import project.truyentranh.model.User;
import project.truyentranh.model.UserStatus;
import project.truyentranh.repository.RoleRepository;
import project.truyentranh.repository.UserRepository;
import project.truyentranh.security.AccessTokenRevocation;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository users;

	private final RoleRepository roles;

	private final PasswordEncoder passwordEncoder;

	private final RefreshTokenService refreshTokens;

	private final AccessTokenRevocation accessTokens;

	private final StorageService storage;

	private final AuditService audit;

	private final UserMapper mapper;

	private final TransactionTemplate tx;

	public User require(long id) {
		return users.findById(id).orElseThrow(() -> ApiException.notFound("người dùng"));
	}

	@Transactional(readOnly = true)
	public UserResponse me(long id) {
		return mapper.toResponse(require(id));
	}

	@Transactional
	public UserResponse updateProfile(long id, String fullName) {
		User user = require(id);
		user.setFullName(fullName.strip());
		return mapper.toResponse(user);
	}

	@Transactional
	public void changePassword(long id, String current, String next, String keepRefreshToken) {
		User user = require(id);
		if (!passwordEncoder.matches(current, user.getPassword())) {
			throw ApiException.badRequest("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng");
		}
		user.setPassword(passwordEncoder.encode(next));
		refreshTokens.revokeAllExcept(id, keepRefreshToken);
	}

	/** Upload nằm ngoài transaction để không giữ kết nối DB khi đang gửi file. */
	public UserResponse changeAvatar(long id, MultipartFile file) {
		String key = storage.store("avatars/" + id, file).key();
		String[] oldKey = new String[1];
		UserResponse result = tx.execute(status -> {
			User user = require(id);
			oldKey[0] = user.getAvatarUrl();
			user.setAvatarUrl(key);
			return mapper.toResponse(user);
		});
		storage.deleteQuietly(oldKey[0]);
		return result;
	}

	@Transactional(readOnly = true)
	public PagedResponse<UserResponse> search(String keyword, int page, int size) {
		return PagedResponse.of(users.search(StringUtils.hasText(keyword) ? keyword.strip() : null,
				PageUtil.of(page, size, Sort.by(Sort.Direction.DESC, "id"))), mapper::toResponse);
	}

	@Transactional
	public UserResponse changeStatus(long actorId, long id, UserStatus status) {
		if (actorId == id) {
			throw ApiException.badRequest("SELF_ACTION", "Không thể tự đổi trạng thái tài khoản của mình");
		}
		User user = require(id);
		UserResponse before = mapper.toResponse(user);
		user.setStatus(status);
		if (status != UserStatus.ACTIVE) {
			refreshTokens.revokeAll(id);
			accessTokens.revokeIssuedBeforeNow(id);
		}
		UserResponse after = mapper.toResponse(user);
		audit.record("USER_STATUS_CHANGE", "USER", id, before, after);
		return after;
	}

	@Transactional
	public UserResponse changeRole(long actorId, long id, String role) {
		if (actorId == id) {
			throw ApiException.badRequest("SELF_ACTION", "Không thể tự đổi quyền của mình");
		}
		User user = require(id);
		UserResponse before = mapper.toResponse(user);
		Set<Role> newRoles = new HashSet<>();
		newRoles.add(roles.findByName("USER").orElseThrow());
		if (role.equals("ADMIN")) {
			newRoles.add(roles.findByName("ADMIN").orElseThrow());
		}
		user.setRoles(newRoles);
		// Buộc đăng nhập lại để token mới mang đúng quyền
		refreshTokens.revokeAll(id);
		accessTokens.revokeIssuedBeforeNow(id);
		UserResponse after = mapper.toResponse(user);
		audit.record("USER_ROLE_CHANGE", "USER", id, before, after);
		return after;
	}

}
