package project.truyentranh.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.NotificationResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.security.CurrentUser;
import project.truyentranh.service.NotificationService;

@RestController
@RequestMapping("/api/v1/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService service;

	@GetMapping
	PagedResponse<NotificationResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.list(CurrentUser.id(), page, size);
	}

	@GetMapping("/unread-count")
	Map<String, Long> unreadCount() {
		return Map.of("count", service.unreadCount(CurrentUser.id()));
	}

	@PatchMapping("/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markRead(@PathVariable long id) {
		service.markRead(CurrentUser.id(), id);
	}

	@PatchMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markAllRead() {
		service.markAllRead(CurrentUser.id());
	}

}
