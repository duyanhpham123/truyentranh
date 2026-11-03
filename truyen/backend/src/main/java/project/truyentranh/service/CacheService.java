package project.truyentranh.service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Cache JSON trên Redis. Redis lỗi thì đọc thẳng từ DB, không làm hỏng request. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

	private final StringRedisTemplate redis;

	private final JsonMapper json;

	public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> type, Supplier<T> loader) {
		try {
			String cached = redis.opsForValue().get(key);
			if (cached != null) {
				return json.readValue(cached, type);
			}
		}
		catch (DataAccessException ex) {
			log.warn("Không đọc được cache {}: {}", key, ex.getMessage());
			return loader.get();
		}
		T value = loader.get();
		try {
			redis.opsForValue().set(key, json.writeValueAsString(value), ttl);
		}
		catch (DataAccessException ex) {
			log.warn("Không ghi được cache {}: {}", key, ex.getMessage());
		}
		return value;
	}

	/** Xóa cache sau khi transaction commit, tránh request khác nạp lại dữ liệu cũ. */
	public void evictAfterCommit(String... keys) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					evict(keys);
				}
			});
		}
		else {
			evict(keys);
		}
	}

	private void evict(String... keys) {
		try {
			redis.delete(List.of(keys));
		}
		catch (DataAccessException ex) {
			log.warn("Không xóa được cache: {}", ex.getMessage());
		}
	}

}
