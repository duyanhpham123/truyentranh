package project.truyentranh.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import project.truyentranh.config.RabbitConfig;
import project.truyentranh.service.NotificationService;

@Component
@RequiredArgsConstructor
public class ChapterPublishedListener {

	private final NotificationService notifications;

	@RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
	public void onChapterPublished(String chapterId) {
		notifications.createForNewChapter(Long.parseLong(chapterId));
	}

}
