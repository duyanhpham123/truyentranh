package project.truyentranh.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
		String frontendUrl,
		String mailFrom,
		Jwt jwt,
		boolean cookieSecure,
		Storage storage,
		BootstrapAdmin bootstrapAdmin) {

	public record Jwt(String secret, Duration accessTtl, Duration refreshTtl) {
	}

	public record Storage(String endpoint, String region, String accessKey, String secretKey, String bucket,
			String publicBaseUrl) {
	}

	public record BootstrapAdmin(String username, String email, String password) {
	}

}
