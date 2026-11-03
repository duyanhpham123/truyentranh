package project.truyentranh.event;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.truyentranh.config.RabbitConfig;
import project.truyentranh.model.OutboxEvent;
import project.truyentranh.repository.OutboxEventRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Event được ghi vào DB cùng transaction với nghiệp vụ, rồi job mới gửi sang RabbitMQ. Nếu broker lỗi, event vẫn còn
 * trong bảng và được gửi lại; consumer idempotent nên gửi trùng cũng không sao.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

	private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);

	private final OutboxEventRepository repository;

	private final RabbitTemplate rabbit;

	private final JsonMapper json;

	@Transactional(propagation = Propagation.MANDATORY)
	public void add(String eventType, long aggregateId) {
		OutboxEvent event = new OutboxEvent();
		event.setEventType(eventType);
		event.setAggregateId(aggregateId);
		event.setPayload(json.writeValueAsString(Map.of("id", aggregateId)));
		repository.save(event);
	}

	@Scheduled(fixedDelayString = "${app.outbox.publish-delay:PT5S}")
	@Transactional
	public void publishPending() {
		for (OutboxEvent event : repository.findTop100BySentAtIsNullOrderByIdAsc()) {
			try {
				rabbit.invoke(ops -> {
					ops.convertAndSend(RabbitConfig.EXCHANGE, event.getEventType(), String.valueOf(event.getAggregateId()));
					ops.waitForConfirmsOrDie(CONFIRM_TIMEOUT.toMillis());
					return null;
				});
				event.setSentAt(Instant.now());
			}
			catch (AmqpException ex) {
				// Giữ đúng thứ tự: dừng tại đây, lần chạy sau gửi lại từ event này
				log.warn("Chưa gửi được event {}: {}", event.getId(), ex.getMessage());
				return;
			}
		}
	}

	@Scheduled(cron = "0 15 3 * * *")
	@Transactional
	public void purgeSent() {
		repository.deleteSentBefore(Instant.now().minus(Duration.ofDays(7)));
	}

}
