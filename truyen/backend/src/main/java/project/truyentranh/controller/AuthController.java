package project.truyentranh.controller;

import java.time.Duration;
import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.truyentranh.config.AppProperties;
import project.truyentranh.dto.request.EmailRequest;
import project.truyentranh.dto.request.LoginRequest;
import project.truyentranh.dto.request.RegisterRequest;
import project.truyentranh.dto.request.ResetPasswordRequest;
import project.truyentranh.dto.request.TokenRequest;
import project.truyentranh.dto.response.AuthResponse;
import project.truyentranh.dto.response.UserResponse;
import project.truyentranh.service.AuthService;
import project.truyentranh.service.RateLimitService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	public static final String REFRESH_COOKIE = "refresh_token";

	private static final String COOKIE_PATH = "/api/v1";

	private final AuthService service;

	private final RateLimitService rateLimit;

	private final AppProperties props;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
		rateLimit.check("register:" + http.getRemoteAddr(), 10, Duration.ofHours(1));
		return service.register(request);
	}

	@PostMapping("/login")
	ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
		String email = request.email().strip().toLowerCase(Locale.ROOT);
		rateLimit.check("login:" + http.getRemoteAddr() + ":" + email, 10, Duration.ofMinutes(15));
		return withSession(service.login(request.email(), request.password()));
	}

	@PostMapping("/refresh")
	ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
		return withSession(service.refresh(token));
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
		service.logout(token);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
	}

	@PostMapping("/forgot-password")
	void forgotPassword(@Valid @RequestBody EmailRequest request, HttpServletRequest http) {
		rateLimit.check("forgot:" + http.getRemoteAddr(), 5, Duration.ofHours(1));
		service.forgotPassword(request.email());
	}

	@PostMapping("/reset-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		service.resetPassword(request.token(), request.newPassword());
	}

	@PostMapping("/verify-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void verifyEmail(@Valid @RequestBody TokenRequest request) {
		service.verifyEmail(request.token());
	}

	@PostMapping("/resend-verification")
	void resendVerification(@Valid @RequestBody EmailRequest request, HttpServletRequest http) {
		rateLimit.check("resend:" + http.getRemoteAddr(), 5, Duration.ofHours(1));
		service.resendVerification(request.email());
	}

	private ResponseEntity<AuthResponse> withSession(AuthService.Session session) {
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie(session.refreshToken(), props.jwt().refreshTtl()).toString())
			.body(new AuthResponse(session.accessToken(), session.expiresIn(), session.user()));
	}

	private ResponseCookie cookie(String value, Duration maxAge) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
			.httpOnly(true)
			.secure(props.cookieSecure())
			.sameSite("Lax")
			.path(COOKIE_PATH)
			.maxAge(maxAge)
			.build();
	}

}
