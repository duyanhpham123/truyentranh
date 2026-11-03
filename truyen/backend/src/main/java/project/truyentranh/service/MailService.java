package project.truyentranh.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import project.truyentranh.config.AppProperties;

/** Gửi bất đồng bộ: request không chờ SMTP, và thời gian phản hồi không lộ email có tồn tại hay không. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

	private final JavaMailSender sender;

	private final AppProperties props;

	@Async
	public void sendVerifyEmail(String to, String token) {
		send(to, "Xác thực email tài khoản Truyện Tranh",
				"Mở liên kết sau để xác thực email (hết hạn sau 24 giờ):\n" + props.frontendUrl() + "/verify-email?token=" + token);
	}

	@Async
	public void sendResetPassword(String to, String token) {
		send(to, "Đặt lại mật khẩu Truyện Tranh",
				"Mở liên kết sau để đặt lại mật khẩu (hết hạn sau 1 giờ):\n" + props.frontendUrl() + "/reset-password?token=" + token
						+ "\n\nNếu bạn không yêu cầu, hãy bỏ qua email này.");
	}

	private void send(String to, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(props.mailFrom());
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		try {
			sender.send(message);
		}
		catch (RuntimeException ex) {
			log.error("Không gửi được mail '{}': {}", subject, ex.getMessage());
		}
	}

}
