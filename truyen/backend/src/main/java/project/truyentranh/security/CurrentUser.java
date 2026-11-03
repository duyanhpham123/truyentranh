package project.truyentranh.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import project.truyentranh.exception.ApiException;

public final class CurrentUser {

	private CurrentUser() {
	}

	/** Id user đã đăng nhập; chỉ gọi ở endpoint yêu cầu xác thực. */
	public static long id() {
		Long id = idOrNull();
		if (id == null) {
			throw ApiException.unauthorized("UNAUTHORIZED", "Cần đăng nhập");
		}
		return id;
	}

	/** Dùng cho endpoint public có thể có hoặc không có token. */
	public static Long idOrNull() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth instanceof JwtAuthenticationToken token ? Long.valueOf(token.getToken().getSubject()) : null;
	}

}
