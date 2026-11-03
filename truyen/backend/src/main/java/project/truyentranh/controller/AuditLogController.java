package project.truyentranh.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.AuditDetailResponse;
import project.truyentranh.dto.response.AuditSummaryResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.service.AuditService;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

	private final AuditService service;

	@GetMapping
	PagedResponse<AuditSummaryResponse> list(@RequestParam(required = false) Long actor,
			@RequestParam(required = false) String action, @RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.search(actor, action, from, to, page, size);
	}

	@GetMapping("/{id}")
	AuditDetailResponse detail(@PathVariable long id) {
		return service.detail(id);
	}

}
