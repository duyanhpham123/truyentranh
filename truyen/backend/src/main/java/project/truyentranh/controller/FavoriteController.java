package project.truyentranh.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.FavoriteResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.security.CurrentUser;
import project.truyentranh.service.FavoriteService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FavoriteController {

	private final FavoriteService service;

	@PostMapping("/comics/{id}/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void follow(@PathVariable long id) {
		service.follow(CurrentUser.id(), id);
	}

	@DeleteMapping("/comics/{id}/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void unfollow(@PathVariable long id) {
		service.unfollow(CurrentUser.id(), id);
	}

	@GetMapping("/me/follows")
	PagedResponse<FavoriteResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.list(CurrentUser.id(), page, size);
	}

}
