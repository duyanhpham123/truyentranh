package project.truyentranh.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	public static final String EXCHANGE = "truyentranh.events";

	public static final String CHAPTER_PUBLISHED = "chapter.published";

	public static final String NOTIFICATION_QUEUE = "notification.chapter-published";

	public static final String DEAD_LETTER_QUEUE = NOTIFICATION_QUEUE + ".dlq";

	@Bean
	TopicExchange eventsExchange() {
		return new TopicExchange(EXCHANGE);
	}

	@Bean
	Queue notificationQueue() {
		return QueueBuilder.durable(NOTIFICATION_QUEUE)
			.deadLetterExchange("")
			.deadLetterRoutingKey(DEAD_LETTER_QUEUE)
			.build();
	}

	@Bean
	Queue notificationDeadLetterQueue() {
		return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
	}

	@Bean
	Binding notificationBinding(Queue notificationQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(notificationQueue).to(eventsExchange).with(CHAPTER_PUBLISHED);
	}

}
