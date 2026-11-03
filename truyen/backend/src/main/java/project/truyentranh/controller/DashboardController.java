package project.truyentranh.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.DailyCountResponse;
import project.truyentranh.dto.response.DashboardSummaryResponse;
import project.truyentranh.dto.response.TopComicResponse;
import project.truyentranh.dto.response.TopGenreResponse;
import project.truyentranh.service.DashboardService;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService service;

	@GetMapping("/summary")
	DashboardSummaryResponse summary() {
		return service.summary();
	}

	@GetMapping("/top-comics")
	List<TopComicResponse> topComics(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "10") int limit) {
		return service.topComics(from, to, limit);
	}

	@GetMapping("/views")
	List<DailyCountResponse> views(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return service.views(from, to);
	}

	@GetMapping("/new-users")
	List<DailyCountResponse> newUsers(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return service.newUsers(from, to);
	}

	@GetMapping("/top-genres")
	List<TopGenreResponse> topGenres(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "10") int limit) {
		return service.topGenres(from, to, limit);
	}

}
