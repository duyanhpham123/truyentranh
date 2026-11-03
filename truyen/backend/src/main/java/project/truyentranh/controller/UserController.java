package project.truyentranh.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.request.ChangePasswordRequest;
import project.truyentranh.dto.request.UpdateProfileRequest;
import project.truyentranh.dto.response.UserResponse;
import project.truyentranh.security.CurrentUser;
import project.truyentranh.service.UserService;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

	private final UserService service;

	@GetMapping
	UserResponse me() {
		return service.me(CurrentUser.id());
	}

	@PatchMapping
	UserResponse update(@Valid @RequestBody UpdateProfileRequest request) {
		return service.updateProfile(CurrentUser.id(), request.fullName());
	}

	@PutMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void changePassword(@Valid @RequestBody ChangePasswordRequest request,
			@CookieValue(name = AuthController.REFRESH_COOKIE, required = false) String refreshToken) {
		service.changePassword(CurrentUser.id(), request.currentPassword(), request.newPassword(), refreshToken);
	}

	@PutMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	UserResponse changeAvatar(@RequestParam MultipartFile file) {
		return service.changeAvatar(CurrentUser.id(), file);
	}

}
