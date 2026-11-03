package project.truyentranh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.ComicDetailResponse;
import project.truyentranh.dto.response.ComicSummaryResponse;
import project.truyentranh.dto.response.MyComicStateResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.model.ComicStatus;
import project.truyentranh.security.CurrentUser;
import project.truyentranh.service.ComicService;

@RestController
@RequestMapping("/api/v1/comics")
@RequiredArgsConstructor
public class ComicController {

	private final ComicService service;

	@GetMapping
	PagedResponse<ComicSummaryResponse> search(@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String genre, @RequestParam(required = false) ComicStatus status,
			@RequestParam(required = false) Long authorId, @RequestParam(defaultValue = "latest") String sort,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size) {
		return service.search(keyword, genre, status, authorId, sort, page, size);
	}

	@GetMapping("/hot")
	List<ComicSummaryResponse> hot(@RequestParam(defaultValue = "week") String period) {
		return service.hot(period);
	}

	@GetMapping("/{slug}")
	ComicDetailResponse detail(@PathVariable String slug) {
		return service.detail(slug);
	}

	@GetMapping("/{id}/me")
	MyComicStateResponse me(@PathVariable long id) {
		return service.myState(CurrentUser.id(), id);
	}

}
