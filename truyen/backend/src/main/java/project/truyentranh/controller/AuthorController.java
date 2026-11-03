package project.truyentranh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.AuthorResponse;
import project.truyentranh.service.AuthorService;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
public class AuthorController {

	private final AuthorService service;

	@GetMapping
	List<AuthorResponse> list() {
		return service.list();
	}

	@GetMapping("/{id}")
	AuthorResponse detail(@PathVariable long id) {
		return service.detail(id);
	}

}
