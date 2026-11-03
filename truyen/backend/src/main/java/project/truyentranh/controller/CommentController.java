package project.truyentranh.controller;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.CommentRequest;
import project.truyentranh.dto.request.ReportRequest;
import project.truyentranh.dto.response.CommentResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.security.CurrentUser;
import project.truyentranh.service.CommentService;
import project.truyentranh.service.RateLimitService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

	private final CommentService service;

	private final RateLimitService rateLimit;

	@GetMapping("/comics/{id}/comments")
	PagedResponse<CommentResponse> list(@PathVariable long id, @RequestParam(required = false) Long chapterId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.list(id, chapterId, page, size);
	}

	@PostMapping("/comics/{id}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	CommentResponse create(@PathVariable long id, @Valid @RequestBody CommentRequest request) {
		long userId = CurrentUser.id();
		rateLimit.check("comment:" + userId, 10, Duration.ofMinutes(1));
		return service.create(userId, id, request);
	}

	@DeleteMapping("/comments/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		service.deleteOwn(CurrentUser.id(), id);
	}

	@PutMapping("/comments/{id}/like")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void like(@PathVariable long id) {
		service.like(CurrentUser.id(), id);
	}

	@DeleteMapping("/comments/{id}/like")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void unlike(@PathVariable long id) {
		service.unlike(CurrentUser.id(), id);
	}

	@PostMapping("/comments/{id}/reports")
	@ResponseStatus(HttpStatus.CREATED)
	void report(@PathVariable long id, @Valid @RequestBody ReportRequest request) {
		service.report(CurrentUser.id(), id, request.reason());
	}

}
