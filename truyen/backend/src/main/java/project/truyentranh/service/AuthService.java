package project.truyentranh.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.truyentranh.config.AppProperties;
import project.truyentranh.dto.request.RegisterRequest;
import project.truyentranh.dto.response.UserResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.UserMapper;
import project.truyentranh.model.Role;
import project.truyentranh.model.TokenType;
import project.truyentranh.model.User;
import project.truyentranh.model.UserStatus;
import project.truyentranh.model.UserToken;
import project.truyentranh.repository.RoleRepository;
import project.truyentranh.repository.UserRepository;
import project.truyentranh.repository.UserTokenRepository;
import project.truyentranh.security.AccessTokenRevocation;
import project.truyentranh.util.TokenUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final Duration VERIFY_TTL = Duration.ofHours(24);

	private static final Duration RESET_TTL = Duration.ofHours(1);

	private final UserRepository users;

	private final RoleRepository roles;

	private final UserTokenRepository userTokens;

	private final RefreshTokenService refreshTokens;

	private final AccessTokenRevocation accessTokens;

	private final PasswordEncoder passwordEncoder;

	private final JwtEncoder jwtEncoder;

	private final MailService mail;

	private final UserMapper userMapper;

	private final AppProperties props;

	public record Session(String accessToken, long expiresIn, UserResponse user, String refreshToken) {
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalize(request.email());
		if (users.existsByEmail(email)) {
			throw ApiException.conflict("EMAIL_TAKEN", "Email đã được sử dụng");
		}
		if (users.existsByUsername(request.username())) {
			throw ApiException.conflict("USERNAME_TAKEN", "Tên đăng nhập đã được sử dụng");
		}
		User user = new User();
		user.setUsername(request.username());
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName().strip());
		user.setRoles(Set.of(roles.findByName("USER").orElseThrow()));
		users.save(user);
		mail.sendVerifyEmail(email, issueToken(user.getId(), TokenType.VERIFY_EMAIL, VERIFY_TTL));
		return userMapper.toResponse(user);
	}

	@Transactional
	public Session login(String email, String password) {
		User user = users.findByEmail(normalize(email))
			.filter(u -> passwordEncoder.matches(password, u.getPassword()))
			.orElseThrow(() -> ApiException.unauthorized("BAD_CREDENTIALS", "Email hoặc mật khẩu không đúng"));
		requireActive(user);
		return newSession(user);
	}

	// noRollbackFor: lệnh thu hồi khi phát hiện dùng lại token phải được lưu dù request trả lỗi 401
	@Transactional(noRollbackFor = ApiException.class)
	public Session refresh(String rawToken) {
		RefreshTokenService.Validation result = refreshTokens.validate(rawToken);
		if (result.check() == RefreshTokenService.Check.REUSED) {
			// Token đã xoay bị dùng lại: có thể đã bị đánh cắp, thu hồi cả phiên đăng nhập của user
			log.warn("Phát hiện dùng lại refresh token của user {}", result.token().getUserId());
			refreshTokens.revokeAll(result.token().getUserId());
		}
		if (result.check() != RefreshTokenService.Check.VALID) {
			throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Phiên đăng nhập đã hết hạn");
		}
		User user = requireUser(result.token().getUserId());
		requireActive(user);
		refreshTokens.revoke(rawToken);
		return newSession(user);
	}

	@Transactional
	public void logout(String rawToken) {
		refreshTokens.revoke(rawToken);
	}

	@Transactional
	public void forgotPassword(String email) {
		users.findByEmail(normalize(email))
			.ifPresent(u -> mail.sendResetPassword(u.getEmail(), issueToken(u.getId(), TokenType.RESET_PASSWORD, RESET_TTL)));
	}

	@Transactional
	public void resetPassword(String token, String newPassword) {
		User user = consumeToken(token, TokenType.RESET_PASSWORD);
		user.setPassword(passwordEncoder.encode(newPassword));
		refreshTokens.revokeAll(user.getId());
		accessTokens.revokeIssuedBeforeNow(user.getId());
	}

	@Transactional
	public void verifyEmail(String token) {
		User user = consumeToken(token, TokenType.VERIFY_EMAIL);
		if (user.getEmailVerifiedAt() == null) {
			user.setEmailVerifiedAt(Instant.now());
		}
	}

	@Transactional
	public void resendVerification(String email) {
		users.findByEmail(normalize(email))
			.filter(u -> u.getEmailVerifiedAt() == null)
			.ifPresent(u -> mail.sendVerifyEmail(u.getEmail(), issueToken(u.getId(), TokenType.VERIFY_EMAIL, VERIFY_TTL)));
	}

	private Session newSession(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(String.valueOf(user.getId()))
			.issuedAt(now)
			.expiresAt(now.plus(props.jwt().accessTtl()))
			.claim("roles", user.getRoles().stream().map(Role::getName).toList())
			.build();
		String access = jwtEncoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
		return new Session(access, props.jwt().accessTtl().toSeconds(), userMapper.toResponse(user),
				refreshTokens.issue(user.getId()));
	}

	private User requireUser(long id) {
		return users.findById(id).orElseThrow(() -> ApiException.notFound("người dùng"));
	}

	private void requireActive(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw ApiException.forbidden("ACCOUNT_LOCKED", "Tài khoản đã bị khóa");
		}
	}

	private String issueToken(long userId, TokenType type, Duration ttl) {
		String raw = TokenUtil.generate();
		UserToken token = new UserToken();
		token.setUserId(userId);
		token.setType(type);
		token.setTokenHash(TokenUtil.hash(raw));
		token.setExpiresAt(Instant.now().plus(ttl));
		userTokens.save(token);
		return raw;
	}

	private User consumeToken(String raw, TokenType type) {
		Instant now = Instant.now();
		UserToken token = userTokens.findByTokenHashAndType(TokenUtil.hash(raw), type)
			.filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(now))
			.orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "Liên kết không hợp lệ hoặc đã hết hạn"));
		token.setUsedAt(now);
		return requireUser(token.getUserId());
	}

	private String normalize(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

}
