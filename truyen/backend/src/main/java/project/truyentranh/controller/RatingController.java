package project.truyentranh.controller;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.RatingRequest;
import project.truyentranh.dto.response.RatingSummaryResponse;
import project.truyentranh.security.CurrentUser;
import project.truyentranh.service.RatingService;

@RestController
@RequestMapping("/api/v1/comics/{id}/rating")
@RequiredArgsConstructor
public class RatingController {

	private final RatingService service;

	@PutMapping
	RatingSummaryResponse rate(@PathVariable long id, @Valid @RequestBody RatingRequest request) {
		return service.rate(CurrentUser.id(), id, request.score());
	}

	@GetMapping
	RatingSummaryResponse summary(@PathVariable long id) {
		return service.summary(id);
	}

}
