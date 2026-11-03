package project.truyentranh.mapper;

import org.springframework.stereotype.Component;

import project.truyentranh.dto.response.NotificationResponse;
import project.truyentranh.model.Notification;

@Component
public class NotificationMapper {

	public NotificationResponse toResponse(Notification n) {
		return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getChapter().getId(),
				n.getChapter().getChapterNumber(), n.getChapter().getComic().getSlug(), n.getReadAt(), n.getCreatedAt());
	}

}
