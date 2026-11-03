package project.truyentranh.config;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.truyentranh.model.User;
import project.truyentranh.repository.RoleRepository;
import project.truyentranh.repository.UserRepository;

/** Tạo admin đầu tiên từ cấu hình app.bootstrap-admin. Dùng cho database chưa có dữ liệu mẫu. */
@Slf4j
@Component
@RequiredArgsConstructor
class AdminBootstrap implements ApplicationRunner {

	private final AppProperties props;

	private final UserRepository users;

	private final RoleRepository roles;

	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(ApplicationArguments args) {
		AppProperties.BootstrapAdmin admin = props.bootstrapAdmin();
		if (admin == null || admin.email() == null || admin.password() == null) {
			return;
		}
		String email = admin.email().toLowerCase(Locale.ROOT);
		if (users.existsByEmail(email)) {
			return;
		}
		User user = new User();
		user.setUsername(admin.username());
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(admin.password()));
		user.setFullName("Admin");
		user.setEmailVerifiedAt(Instant.now());
		user.setRoles(Set.of(roles.findByName("USER").orElseThrow(), roles.findByName("ADMIN").orElseThrow()));
		users.save(user);
		log.info("Đã tạo tài khoản admin đầu tiên: {}", email);
	}

}
