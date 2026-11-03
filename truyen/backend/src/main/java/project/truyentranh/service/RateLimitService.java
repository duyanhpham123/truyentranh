package project.truyentranh.service;

import java.time.Duration;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.truyentranh.exception.ApiException;

/** Giới hạn theo cửa sổ cố định. Redis lỗi thì cho qua để không chặn người dùng thật. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

	private final StringRedisTemplate redis;

	public void check(String key, int limit, Duration window) {
		String redisKey = "ratelimit:" + key;
		Long count;
		try {
			count = redis.opsForValue().increment(redisKey);
			if (count != null && count == 1) {
				redis.expire(redisKey, window);
			}
		}
		catch (DataAccessException ex) {
			log.warn("Rate limit bỏ qua vì Redis lỗi: {}", ex.getMessage());
			return;
		}
		if (count != null && count > limit) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Bạn thao tác quá nhanh, hãy thử lại sau");
		}
	}

}
