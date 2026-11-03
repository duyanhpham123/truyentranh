package project.truyentranh.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.ComicRequest;
import project.truyentranh.dto.request.VisibilityRequest;
import project.truyentranh.dto.response.ComicDetailResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.service.ComicService;

@RestController
@RequestMapping("/api/v1/admin/comics")
@RequiredArgsConstructor
public class AdminComicController {

	private final ComicService service;

	@GetMapping
	PagedResponse<ComicDetailResponse> list(@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.adminList(keyword, page, size);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ComicDetailResponse create(@Valid @RequestBody ComicRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	ComicDetailResponse update(@PathVariable long id, @Valid @RequestBody ComicRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		service.delete(id);
	}

	@PatchMapping("/{id}/visibility")
	ComicDetailResponse visibility(@PathVariable long id, @Valid @RequestBody VisibilityRequest request) {
		return service.setHidden(id, request.hidden());
	}

	@PutMapping(path = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ComicDetailResponse cover(@PathVariable long id, @RequestParam MultipartFile file) {
		return service.changeCover(id, file);
	}

}
