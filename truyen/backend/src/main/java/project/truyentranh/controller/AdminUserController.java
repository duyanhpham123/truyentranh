package project.truyentranh.controller;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.UserRoleRequest;
import project.truyentranh.dto.request.UserStatusRequest;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.dto.response.UserResponse;
import project.truyentranh.security.CurrentUser;
import project.truyentranh.service.UserService;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final UserService service;

	@GetMapping
	PagedResponse<UserResponse> list(@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return service.search(keyword, page, size);
	}

	@PatchMapping("/{id}/status")
	UserResponse changeStatus(@PathVariable long id, @Valid @RequestBody UserStatusRequest request) {
		return service.changeStatus(CurrentUser.id(), id, request.status());
	}

	@PatchMapping("/{id}/role")
	UserResponse changeRole(@PathVariable long id, @Valid @RequestBody UserRoleRequest request) {
		return service.changeRole(CurrentUser.id(), id, request.role());
	}

}
