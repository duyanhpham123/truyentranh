package project.truyentranh.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.truyentranh.config.AppProperties;
import project.truyentranh.model.RefreshToken;
import project.truyentranh.repository.RefreshTokenRepository;
import project.truyentranh.util.TokenUtil;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	/** Nhiều tab refresh cùng lúc sẽ dùng lại token vừa xoay, trong khoảng này không coi là bị đánh cắp. */
	private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

	private final RefreshTokenRepository repository;

	private final AppProperties props;

	public enum Check {
		VALID, INVALID, REUSED
	}

	public record Validation(Check check, RefreshToken token) {
	}

	/** Trả về token gốc để đặt vào cookie; DB chỉ lưu hash. */
	@Transactional
	public String issue(long userId) {
		String raw = TokenUtil.generate();
		RefreshToken token = new RefreshToken();
		token.setUserId(userId);
		token.setTokenHash(TokenUtil.hash(raw));
		token.setExpiresAt(Instant.now().plus(props.jwt().refreshTtl()));
		repository.save(token);
		return raw;
	}

	@Transactional(readOnly = true)
	public Validation validate(String raw) {
		if (raw == null) {
			return new Validation(Check.INVALID, null);
		}
		RefreshToken token = repository.findByTokenHash(TokenUtil.hash(raw)).orElse(null);
		Instant now = Instant.now();
		if (token == null || token.getExpiresAt().isBefore(now)) {
			return new Validation(Check.INVALID, null);
		}
		if (token.getRevokedAt() == null) {
			return new Validation(Check.VALID, token);
		}
		boolean withinGrace = token.getRevokedAt().plus(REUSE_GRACE).isAfter(now);
		return new Validation(withinGrace ? Check.INVALID : Check.REUSED, token);
	}

	@Transactional
	public void revoke(String raw) {
		if (raw != null) {
			repository.findByTokenHash(TokenUtil.hash(raw)).ifPresent(t -> {
				if (t.getRevokedAt() == null) {
					t.setRevokedAt(Instant.now());
				}
			});
		}
	}

	@Transactional
	public void revokeAll(long userId) {
		repository.revokeAll(userId, null, Instant.now());
	}

	@Transactional
	public void revokeAllExcept(long userId, String keepRaw) {
		repository.revokeAll(userId, keepRaw == null ? null : TokenUtil.hash(keepRaw), Instant.now());
	}

	@Scheduled(cron = "0 30 3 * * *")
	@Transactional
	public void purgeExpired() {
		repository.deleteExpired(Instant.now());
	}

}
