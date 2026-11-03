package project.truyentranh.security;

import java.time.Instant;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.truyentranh.config.AppProperties;

/**
 * Vô hiệu ngay các access token đã cấp của một user (khóa tài khoản, đổi quyền, đặt lại mật khẩu), không phải chờ
 * token hết hạn. Khóa Redis chỉ cần sống bằng thời hạn access token. Redis lỗi thì cho qua để không chặn mọi request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenRevocation implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error REVOKED = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Token đã bị thu hồi", null);

	private final StringRedisTemplate redis;

	private final AppProperties props;

	public void revokeIssuedBeforeNow(long userId) {
		try {
			redis.opsForValue().set(key(userId), String.valueOf(Instant.now().getEpochSecond()), props.jwt().accessTtl());
		}
		catch (DataAccessException ex) {
			log.warn("Không thu hồi được access token của user {}: {}", userId, ex.getMessage());
		}
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		try {
			String revokedAt = redis.opsForValue().get(key(Long.parseLong(token.getSubject())));
			if (revokedAt != null && token.getIssuedAt().getEpochSecond() < Long.parseLong(revokedAt)) {
				return OAuth2TokenValidatorResult.failure(REVOKED);
			}
		}
		catch (DataAccessException ex) {
			log.warn("Không kiểm tra được thu hồi token: {}", ex.getMessage());
		}
		return OAuth2TokenValidatorResult.success();
	}

	private String key(long userId) {
		return "auth:revoked-before:" + userId;
	}

}
