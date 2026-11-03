package project.truyentranh.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.GenreRequest;
import project.truyentranh.dto.response.GenreResponse;
import project.truyentranh.service.GenreService;

@RestController
@RequestMapping("/api/v1/admin/genres")
@RequiredArgsConstructor
public class AdminGenreController {

	private final GenreService service;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	GenreResponse create(@Valid @RequestBody GenreRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	GenreResponse update(@PathVariable long id, @Valid @RequestBody GenreRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		service.delete(id);
	}

}
