package project.truyentranh.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.AuthorRequest;
import project.truyentranh.dto.response.AuthorResponse;
import project.truyentranh.service.AuthorService;

@RestController
@RequestMapping("/api/v1/admin/authors")
@RequiredArgsConstructor
public class AdminAuthorController {

	private final AuthorService service;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AuthorResponse create(@Valid @RequestBody AuthorRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	AuthorResponse update(@PathVariable long id, @Valid @RequestBody AuthorRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		service.delete(id);
	}

}
