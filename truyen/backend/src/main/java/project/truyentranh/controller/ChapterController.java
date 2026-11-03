package project.truyentranh.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.ChapterReadResponse;
import project.truyentranh.dto.response.ChapterSummaryResponse;
import project.truyentranh.security.CurrentUser;
import project.truyentranh.service.ChapterService;
import project.truyentranh.service.ViewService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChapterController {

	private final ChapterService service;

	private final ViewService views;

	@GetMapping("/comics/{slug}/chapters")
	List<ChapterSummaryResponse> list(@PathVariable String slug) {
		return service.list(slug);
	}

	@GetMapping("/comics/{slug}/chapters/{number}")
	ChapterReadResponse read(@PathVariable String slug, @PathVariable BigDecimal number) {
		return service.read(slug, number);
	}

	@PostMapping("/chapters/{id}/views")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void recordView(@PathVariable long id, HttpServletRequest request) {
		Long userId = CurrentUser.idOrNull();
		views.record(id, userId != null ? "u" + userId : "ip" + request.getRemoteAddr());
	}

}
