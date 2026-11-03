package project.truyentranh.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.ChapterRequest;
import project.truyentranh.dto.request.ReorderPagesRequest;
import project.truyentranh.dto.response.AdminChapterResponse;
import project.truyentranh.service.ChapterService;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminChapterController {

	private final ChapterService service;

	@GetMapping("/comics/{id}/chapters")
	List<AdminChapterResponse> list(@PathVariable long id) {
		return service.adminList(id);
	}

	@PostMapping("/comics/{id}/chapters")
	@ResponseStatus(HttpStatus.CREATED)
	AdminChapterResponse create(@PathVariable long id, @Valid @RequestBody ChapterRequest request) {
		return service.create(id, request);
	}

	@PutMapping("/chapters/{id}")
	AdminChapterResponse update(@PathVariable long id, @Valid @RequestBody ChapterRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/chapters/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		service.delete(id);
	}

	@PostMapping(path = "/chapters/{id}/pages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	AdminChapterResponse uploadPages(@PathVariable long id, @RequestParam List<MultipartFile> files) {
		return service.uploadPages(id, files);
	}

	@PutMapping("/chapters/{id}/pages")
	AdminChapterResponse reorderPages(@PathVariable long id, @Valid @RequestBody ReorderPagesRequest request) {
		return service.reorderPages(id, request.pageIds());
	}

	@PostMapping("/chapters/{id}/publish")
	AdminChapterResponse publish(@PathVariable long id) {
		return service.publish(id);
	}

}
