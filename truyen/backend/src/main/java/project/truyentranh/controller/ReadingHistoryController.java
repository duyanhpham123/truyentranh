package project.truyentranh.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.ProgressRequest;
import project.truyentranh.dto.response.HistoryResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.security.CurrentUser;
import project.truyentranh.service.ReadingHistoryService;

@RestController
@RequestMapping("/api/v1/me/history")
@RequiredArgsConstructor
public class ReadingHistoryController {

	private final ReadingHistoryService service;

	@PutMapping("/{comicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void saveProgress(@PathVariable long comicId, @Valid @RequestBody ProgressRequest request) {
		service.saveProgress(CurrentUser.id(), comicId, request.chapterId(), request.page());
	}

	@GetMapping
	PagedResponse<HistoryResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.list(CurrentUser.id(), page, size);
	}

	@DeleteMapping("/{comicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long comicId) {
		service.delete(CurrentUser.id(), comicId);
	}

}
