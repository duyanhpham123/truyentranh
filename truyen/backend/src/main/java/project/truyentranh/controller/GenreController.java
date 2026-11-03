package project.truyentranh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.GenreResponse;
import project.truyentranh.service.GenreService;

@RestController
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
public class GenreController {

	private final GenreService service;

	@GetMapping
	List<GenreResponse> list() {
		return service.list();
	}

	@GetMapping("/{slug}")
	GenreResponse detail(@PathVariable String slug) {
		return service.detail(slug);
	}

}
