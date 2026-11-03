package project.truyentranh.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.ResolveReportRequest;
import project.truyentranh.dto.response.CommentReportResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.model.ReportStatus;
import project.truyentranh.service.CommentService;

@RestController
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

	private final CommentService service;

	@GetMapping("/reports")
	PagedResponse<CommentReportResponse> reports(@RequestParam(defaultValue = "PENDING") ReportStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.reports(status, page, size);
	}

	@PatchMapping("/reports/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void resolve(@PathVariable long id, @Valid @RequestBody ResolveReportRequest request) {
		service.resolveReport(id, request.status());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void hide(@PathVariable long id) {
		service.adminHide(id);
	}

}
