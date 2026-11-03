package project.truyentranh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;
import project.truyentranh.config.RabbitConfig;
import project.truyentranh.service.ViewService;

/** Chạy trên MySQL, Redis, RabbitMQ, MinIO thật (Testcontainers); chỉ giả lập phần gửi mail. */
@SpringBootTest(properties = { "app.outbox.publish-delay=PT0.2S", "app.bootstrap-admin.username=admin",
		"app.bootstrap-admin.email=admin@test.local", "app.bootstrap-admin.password=Admin@12345" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TruyentranhApiApplicationTests {

	private static final String API = "/api/v1";

	private static final Pattern TOKEN = Pattern.compile("token=([\\w-]+)");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ViewService viewService;

	@Autowired
	private RabbitTemplate rabbit;

	@MockitoBean
	private JavaMailSender mailSender;

	@Test
	void authFlowsAndPermissions() throws Exception {
		call(HttpMethod.POST, "/auth/register", null, """
				{"username":"a b","email":"bad","password":"short","fullName":""}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

		register("authuser", "auth@test.local", "Người Dùng");
		call(HttpMethod.POST, "/auth/register", null, registerBody("authuser2", "AUTH@test.local", "Khác"))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("EMAIL_TAKEN")));
		call(HttpMethod.POST, "/auth/register", null, registerBody("authuser", "khac@test.local", "Khác"))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("USERNAME_TAKEN")));

		call(HttpMethod.POST, "/auth/login", null, loginBody("auth@test.local", "sai-mat-khau")).andExpect(status().isUnauthorized());
		MvcResult login = call(HttpMethod.POST, "/auth/login", null, loginBody("auth@test.local", "Password@123"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.user.roles", contains("USER"))).andReturn();
		String access = field(login, "$.accessToken");
		Cookie refresh = login.getResponse().getCookie("refresh_token");
		assertThat(refresh).isNotNull();
		assertThat(login.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("HttpOnly", "SameSite=Lax");

		// Phân quyền: khách và user thường không vào được khu vực riêng
		call(HttpMethod.GET, "/users/me", null, null).andExpect(status().isUnauthorized());
		call(HttpMethod.GET, "/admin/users", access, null).andExpect(status().isForbidden());
		call(HttpMethod.GET, "/users/me", access, null).andExpect(jsonPath("$.emailVerified", is(false)));

		// Xoay vòng refresh token
		MvcResult rotated = mvc.perform(request(HttpMethod.POST, API + "/auth/refresh").cookie(refresh))
			.andExpect(status().isOk()).andReturn();
		Cookie newRefresh = rotated.getResponse().getCookie("refresh_token");
		assertThat(newRefresh.getValue()).isNotEqualTo(refresh.getValue());

		// Đổi profile, mật khẩu, avatar
		call(HttpMethod.PATCH, "/users/me", access, """
				{"fullName":"Tên Mới"}""").andExpect(jsonPath("$.fullName", is("Tên Mới")));
		call(HttpMethod.PUT, "/users/me/password", access, """
				{"currentPassword":"sai-mat-khau","newPassword":"Password@456"}""").andExpect(status().isBadRequest());
		call(HttpMethod.PUT, "/users/me/password", access, """
				{"currentPassword":"Password@123","newPassword":"Password@456"}""").andExpect(status().isNoContent());
		upload(HttpMethod.PUT, "/users/me/avatar", access, "file", png()).andExpect(status().isOk());
		upload(HttpMethod.PUT, "/users/me/avatar", access, "file", "day-khong-phai-anh".getBytes())
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("INVALID_IMAGE")));

		// Quên mật khẩu: token dùng được đúng một lần, email không tồn tại vẫn trả 200
		call(HttpMethod.POST, "/auth/forgot-password", null, """
				{"email":"khong-ton-tai@test.local"}""").andExpect(status().isOk());
		call(HttpMethod.POST, "/auth/forgot-password", null, """
				{"email":"auth@test.local"}""").andExpect(status().isOk());
		String resetBody = """
				{"token":"%s","newPassword":"Password@789"}""".formatted(tokenFromMail("auth@test.local", "Đặt lại mật khẩu"));
		call(HttpMethod.POST, "/auth/reset-password", null, resetBody).andExpect(status().isNoContent());
		call(HttpMethod.POST, "/auth/reset-password", null, resetBody).andExpect(status().isBadRequest());

		// Reset mật khẩu đã thu hồi mọi refresh token
		mvc.perform(request(HttpMethod.POST, API + "/auth/refresh").cookie(newRefresh)).andExpect(status().isUnauthorized());

		MvcResult relogin = call(HttpMethod.POST, "/auth/login", null, loginBody("auth@test.local", "Password@789"))
			.andExpect(status().isOk()).andReturn();
		mvc.perform(request(HttpMethod.POST, API + "/auth/logout")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + field(relogin, "$.accessToken"))
			.cookie(relogin.getResponse().getCookie("refresh_token"))).andExpect(status().isNoContent());
		mvc.perform(request(HttpMethod.POST, API + "/auth/refresh").cookie(relogin.getResponse().getCookie("refresh_token")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void fullReadingAndAdminFlow() throws Exception {
		String admin = login("admin@test.local", "Admin@12345");
		register("reader", "reader@test.local", "Độc Giả");
		String reader = login("reader@test.local", "Password@123");

		// Tác giả
		long authorId = id(call(HttpMethod.POST, "/admin/authors", admin, """
				{"name":"Tác Giả Flow","biography":"tieu su"}""").andExpect(status().isCreated()));
		long spareAuthor = id(call(HttpMethod.POST, "/admin/authors", admin, """
				{"name":"Tác Giả Tạm"}""").andExpect(status().isCreated()));
		call(HttpMethod.PUT, "/admin/authors/" + spareAuthor, admin, """
				{"name":"Tác Giả Sửa"}""").andExpect(jsonPath("$.name", is("Tác Giả Sửa")));
		call(HttpMethod.GET, "/authors", null, null).andExpect(status().isOk());
		call(HttpMethod.GET, "/authors/" + authorId, null, null).andExpect(jsonPath("$.biography", is("tieu su")));
		call(HttpMethod.DELETE, "/admin/authors/" + spareAuthor, admin, null).andExpect(status().isNoContent());

		// Thể loại
		long genreId = id(call(HttpMethod.POST, "/admin/genres", admin, """
				{"name":"Hành Động Flow","description":"mo ta"}""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug", is("hanh-dong-flow"))));
		call(HttpMethod.GET, "/genres", null, null).andExpect(status().isOk());
		call(HttpMethod.GET, "/genres/hanh-dong-flow", null, null).andExpect(status().isOk());
		long spareGenre = id(call(HttpMethod.POST, "/admin/genres", admin, """
				{"name":"Tạm Thời Flow"}""").andExpect(status().isCreated()));
		call(HttpMethod.PUT, "/admin/genres/" + spareGenre, admin, """
				{"name":"Tạm Thời Sửa"}""").andExpect(jsonPath("$.name", is("Tạm Thời Sửa")));
		call(HttpMethod.DELETE, "/admin/genres/" + spareGenre, admin, null).andExpect(status().isNoContent());

		// Truyện
		String comicBody = """
				{"title":"Đấu Phá Flow","authorId":%d,"description":"mo ta","status":"ONGOING","genreIds":[%d]}""".formatted(authorId, genreId);
		long comicId = id(call(HttpMethod.POST, "/admin/comics", admin, comicBody).andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug", is("dau-pha-flow"))));
		String slug = "dau-pha-flow";
		call(HttpMethod.POST, "/admin/comics", admin, comicBody).andExpect(jsonPath("$.slug", is("dau-pha-flow-2")));
		call(HttpMethod.PUT, "/admin/comics/" + comicId, admin, comicBody).andExpect(status().isOk());
		String cover = field(upload(HttpMethod.PUT, "/admin/comics/" + comicId + "/cover", admin, "file", png())
			.andExpect(status().isOk()).andReturn(), "$.coverUrl");
		assertThat(httpStatus(cover)).as("ảnh bìa đọc được công khai").isEqualTo(200);
		call(HttpMethod.GET, "/admin/comics", admin, null).andExpect(status().isOk());
		call(HttpMethod.DELETE, "/admin/genres/" + genreId, admin, null).andExpect(status().isConflict());
		call(HttpMethod.DELETE, "/admin/authors/" + authorId, admin, null).andExpect(status().isConflict());

		// Chapter nháp không hiện với độc giả và chưa publish được khi chưa có trang
		long chapterId = id(call(HttpMethod.POST, "/admin/comics/" + comicId + "/chapters", admin, """
				{"chapterNumber":1,"chapterTitle":"Mở đầu"}""").andExpect(status().isCreated()).andExpect(jsonPath("$.status", is("DRAFT"))));
		call(HttpMethod.POST, "/admin/comics/" + comicId + "/chapters", admin, """
				{"chapterNumber":1}""").andExpect(status().isConflict());
		call(HttpMethod.GET, "/comics/" + slug + "/chapters", null, null).andExpect(jsonPath("$", hasSize(0)));
		call(HttpMethod.GET, "/comics/" + slug + "/chapters/1", null, null).andExpect(status().isNotFound());
		call(HttpMethod.POST, "/admin/chapters/" + chapterId + "/publish", admin, null).andExpect(status().isBadRequest());

		// Upload 3 trang, bỏ trang 2 và đảo thứ tự (unique theo chapter + số trang)
		MvcResult uploaded = mvc.perform(multipart(API + "/admin/chapters/" + chapterId + "/pages")
			.file(image("files")).file(image("files")).file(image("files"))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
			.andExpect(status().isOk()).andExpect(jsonPath("$.pages", hasSize(3)))
			.andExpect(jsonPath("$.pages[0].width", is(4))).andReturn();
		List<Integer> pageIds = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.pages[*].id");
		String removedUrl = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.pages[1].imageUrl");
		call(HttpMethod.PUT, "/admin/chapters/" + chapterId + "/pages", admin, """
				{"pageIds":[%d,%d]}""".formatted(pageIds.get(2), pageIds.get(0))).andExpect(jsonPath("$.pages", hasSize(2)))
			.andExpect(jsonPath("$.pages[0].id", is(pageIds.get(2)))).andExpect(jsonPath("$.pages[0].pageNumber", is(1)));
		assertThat(httpStatus(removedUrl)).as("ảnh của trang đã bỏ bị xóa").isEqualTo(404);
		call(HttpMethod.PUT, "/admin/chapters/" + chapterId, admin, """
				{"chapterNumber":1,"chapterTitle":"Mở đầu mới"}""").andExpect(jsonPath("$.chapterTitle", is("Mở đầu mới")));

		// Người theo dõi nhận thông báo sau khi publish, đi qua outbox và RabbitMQ
		call(HttpMethod.POST, "/comics/" + comicId + "/follow", reader, null).andExpect(status().isNoContent());
		call(HttpMethod.POST, "/comics/" + comicId + "/follow", reader, null).andExpect(status().isNoContent());
		call(HttpMethod.POST, "/admin/chapters/" + chapterId + "/publish", admin, null).andExpect(jsonPath("$.status", is("PUBLISHED")));
		call(HttpMethod.POST, "/admin/chapters/" + chapterId + "/publish", admin, null).andExpect(status().isConflict());
		await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> call(HttpMethod.GET, "/me/notifications/unread-count", reader, null)
			.andExpect(jsonPath("$.count", is(1))));
		MvcResult notifications = call(HttpMethod.GET, "/me/notifications", reader, null)
			.andExpect(jsonPath("$.items[0].message", is("Đấu Phá Flow đã có chapter 1"))).andReturn();
		int notificationId = JsonPath.read(notifications.getResponse().getContentAsString(), "$.items[0].id");
		call(HttpMethod.PATCH, "/me/notifications/" + notificationId + "/read", reader, null).andExpect(status().isNoContent());
		call(HttpMethod.PATCH, "/me/notifications/read-all", reader, null).andExpect(status().isNoContent());
		call(HttpMethod.GET, "/me/notifications/unread-count", reader, null).andExpect(jsonPath("$.count", is(0)));
		call(HttpMethod.PATCH, "/me/notifications/" + notificationId + "/read", admin, null).andExpect(status().isNotFound());

		// Đọc truyện không cần đăng nhập
		call(HttpMethod.GET, "/comics", null, null).andExpect(status().isOk());
		call(HttpMethod.GET, "/comics?keyword=dau pha flow", null, null).andExpect(jsonPath("$.totalItems", is(2)));
		call(HttpMethod.GET, "/comics?keyword=tac gia flow", null, null).andExpect(jsonPath("$.totalItems", is(2)));
		call(HttpMethod.GET, "/comics?authorId=" + authorId, null, null).andExpect(jsonPath("$.totalItems", is(2)));
		call(HttpMethod.GET, "/comics?genre=hanh-dong-flow&sort=views", null, null).andExpect(jsonPath("$.totalItems", is(2)));
		call(HttpMethod.GET, "/comics?sort=sai", null, null).andExpect(status().isBadRequest());
		call(HttpMethod.GET, "/comics/" + slug, null, null).andExpect(jsonPath("$.genres[0].name", is("Hành Động Flow")))
			.andExpect(jsonPath("$.authorName", is("Tác Giả Flow")));
		call(HttpMethod.GET, "/comics/" + slug + "/chapters", null, null).andExpect(jsonPath("$", hasSize(1)));

		long chapter2 = id(call(HttpMethod.POST, "/admin/comics/" + comicId + "/chapters", admin, """
				{"chapterNumber":1.5}""").andExpect(status().isCreated()));
		upload(HttpMethod.POST, "/admin/chapters/" + chapter2 + "/pages", admin, "files", png()).andExpect(status().isOk());
		call(HttpMethod.POST, "/admin/chapters/" + chapter2 + "/publish", admin, null).andExpect(status().isOk());
		call(HttpMethod.GET, "/comics/" + slug + "/chapters/1", null, null).andExpect(jsonPath("$.pages", hasSize(2)))
			.andExpect(jsonPath("$.nextChapter", is(1.5)));
		call(HttpMethod.GET, "/comics/" + slug + "/chapters/1.5", null, null).andExpect(jsonPath("$.prevChapter", is(1.0)));

		// Xóa chapter rồi tạo lại đúng số đó phải được
		long draft = id(call(HttpMethod.POST, "/admin/comics/" + comicId + "/chapters", admin, """
				{"chapterNumber":9}""").andExpect(status().isCreated()));
		call(HttpMethod.DELETE, "/admin/chapters/" + draft, admin, null).andExpect(status().isNoContent());
		call(HttpMethod.POST, "/admin/comics/" + comicId + "/chapters", admin, """
				{"chapterNumber":9}""").andExpect(status().isCreated());
		call(HttpMethod.GET, "/admin/comics/" + comicId + "/chapters", admin, null).andExpect(jsonPath("$", hasSize(3)));

		// View: cùng một người chỉ tính 1 lần, gom xuống DB rồi lên bảng xếp hạng và dashboard
		call(HttpMethod.POST, "/chapters/" + chapterId + "/views", reader, null).andExpect(status().isNoContent());
		call(HttpMethod.POST, "/chapters/" + chapterId + "/views", reader, null).andExpect(status().isNoContent());
		call(HttpMethod.POST, "/chapters/" + chapterId + "/views", admin, null).andExpect(status().isNoContent());
		call(HttpMethod.POST, "/chapters/999999/views", null, null).andExpect(status().isNotFound());
		viewService.flush();
		call(HttpMethod.GET, "/comics/hot?period=day", null, null).andExpect(jsonPath("$[0].id", is((int) comicId)));
		call(HttpMethod.GET, "/comics/hot?period=sai", null, null).andExpect(status().isBadRequest());
		call(HttpMethod.GET, "/admin/dashboard/summary", admin, null).andExpect(jsonPath("$.viewsToday", is(2)));
		call(HttpMethod.GET, "/admin/dashboard/top-comics", admin, null).andExpect(jsonPath("$[0].views", is(2)));
		call(HttpMethod.GET, "/admin/dashboard/views", admin, null).andExpect(jsonPath("$[0].count", is(2)));
		call(HttpMethod.GET, "/admin/dashboard/new-users", admin, null).andExpect(status().isOk());
		call(HttpMethod.GET, "/admin/dashboard/top-genres", admin, null).andExpect(jsonPath("$[0].name", is("Hành Động Flow")));

		// Lịch sử đọc: mỗi truyện một dòng, chapter phải thuộc đúng truyện
		call(HttpMethod.PUT, "/me/history/" + comicId, reader, """
				{"chapterId":%d,"page":2}""".formatted(chapterId)).andExpect(status().isNoContent());
		call(HttpMethod.PUT, "/me/history/" + comicId, reader, """
				{"chapterId":%d,"page":1}""".formatted(chapter2)).andExpect(status().isNoContent());
		call(HttpMethod.PUT, "/me/history/" + comicId, reader, """
				{"chapterId":999999,"page":1}""").andExpect(status().isNotFound());
		call(HttpMethod.GET, "/me/history", reader, null).andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].chapterNumber", is(1.5)));
		call(HttpMethod.GET, "/me/follows", reader, null).andExpect(jsonPath("$.items", hasSize(1)));

		// Chấm điểm
		call(HttpMethod.PUT, "/comics/" + comicId + "/rating", reader, """
				{"score":6}""").andExpect(status().isBadRequest());
		call(HttpMethod.PUT, "/comics/" + comicId + "/rating", reader, """
				{"score":2}""").andExpect(status().isOk());
		call(HttpMethod.PUT, "/comics/" + comicId + "/rating", admin, """
				{"score":5}""").andExpect(status().isOk());
		call(HttpMethod.PUT, "/comics/" + comicId + "/rating", reader, """
				{"score":4}""").andExpect(jsonPath("$.count", is(2))).andExpect(jsonPath("$.average", is(4.5)));
		call(HttpMethod.GET, "/comics/" + comicId + "/rating", null, null).andExpect(jsonPath("$.average", is(4.5)));
		call(HttpMethod.GET, "/comics/" + slug, null, null).andExpect(jsonPath("$.ratingAvg", is(4.5)));

		call(HttpMethod.GET, "/comics/" + comicId + "/me", null, null).andExpect(status().isUnauthorized());
		call(HttpMethod.GET, "/comics/" + comicId + "/me", reader, null).andExpect(jsonPath("$.following", is(true)))
			.andExpect(jsonPath("$.myRating", is(4))).andExpect(jsonPath("$.lastRead.page", is(1)));

		// Bình luận: phải xác thực email trước
		String commentBody = """
				{"content":"Truyện hay"}""";
		call(HttpMethod.POST, "/comics/" + comicId + "/comments", reader, commentBody).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code", is("EMAIL_NOT_VERIFIED")));
		call(HttpMethod.POST, "/auth/resend-verification", null, """
				{"email":"reader@test.local"}""").andExpect(status().isOk());
		call(HttpMethod.POST, "/auth/verify-email", null, """
				{"token":"%s"}""".formatted(tokenFromMail("reader@test.local", "Xác thực"))).andExpect(status().isNoContent());
		call(HttpMethod.POST, "/auth/verify-email", null, """
				{"token":"sai"}""").andExpect(status().isBadRequest());

		long commentId = id(call(HttpMethod.POST, "/comics/" + comicId + "/comments", reader, commentBody).andExpect(status().isCreated()));
		long replyId = id(call(HttpMethod.POST, "/comics/" + comicId + "/comments", admin, """
				{"content":"Đồng ý","parentId":%d}""".formatted(commentId)).andExpect(status().isCreated()));
		call(HttpMethod.POST, "/comics/" + comicId + "/comments", reader, """
				{"content":"Lồng 2 cấp","parentId":%d}""".formatted(replyId)).andExpect(status().isBadRequest());
		call(HttpMethod.PUT, "/comments/" + commentId + "/like", admin, null).andExpect(status().isNoContent());
		call(HttpMethod.PUT, "/comments/" + commentId + "/like", admin, null).andExpect(status().isNoContent());
		call(HttpMethod.GET, "/comics/" + comicId + "/comments", null, null).andExpect(jsonPath("$.items[0].likeCount", is(1)))
			.andExpect(jsonPath("$.items[0].replies", hasSize(1)))
			.andExpect(jsonPath("$.items[0].author.username", is("reader")));
		call(HttpMethod.DELETE, "/comments/" + commentId + "/like", admin, null).andExpect(status().isNoContent());
		call(HttpMethod.DELETE, "/comments/" + commentId + "/like", admin, null).andExpect(status().isNoContent());
		call(HttpMethod.GET, "/comics/" + comicId + "/comments", null, null).andExpect(jsonPath("$.items[0].likeCount", is(0)));
		call(HttpMethod.DELETE, "/comments/" + commentId, admin, null).andExpect(status().isForbidden());

		// Báo cáo và kiểm duyệt
		String reportBody = """
				{"reason":"Spam"}""";
		call(HttpMethod.POST, "/comments/" + replyId + "/reports", reader, reportBody).andExpect(status().isCreated());
		call(HttpMethod.POST, "/comments/" + replyId + "/reports", reader, reportBody).andExpect(status().isConflict());
		call(HttpMethod.POST, "/comments/" + commentId + "/reports", admin, reportBody).andExpect(status().isCreated());
		MvcResult reports = call(HttpMethod.GET, "/admin/comments/reports", admin, null).andExpect(jsonPath("$.totalItems", is(2))).andReturn();
		int reportId = JsonPath.read(reports.getResponse().getContentAsString(), "$.items[0].id");
		call(HttpMethod.PATCH, "/admin/comments/reports/" + reportId, admin, """
				{"status":"DISMISSED"}""").andExpect(status().isNoContent());
		call(HttpMethod.DELETE, "/admin/comments/" + replyId, admin, null).andExpect(status().isNoContent());
		call(HttpMethod.GET, "/admin/comments/reports", admin, null).andExpect(jsonPath("$.totalItems", is(0)));
		call(HttpMethod.DELETE, "/comments/" + commentId, reader, null).andExpect(status().isNoContent());
		call(HttpMethod.GET, "/comics/" + comicId + "/comments", null, null).andExpect(jsonPath("$.items", hasSize(0)));

		// Quản trị user: khóa thì không đăng nhập được, không tự khóa mình
		call(HttpMethod.DELETE, "/me/history/" + comicId, reader, null).andExpect(status().isNoContent());
		call(HttpMethod.DELETE, "/comics/" + comicId + "/follow", reader, null).andExpect(status().isNoContent());
		MvcResult users = call(HttpMethod.GET, "/admin/users?keyword=reader@test.local", admin, null).andExpect(status().isOk()).andReturn();
		int readerId = JsonPath.read(users.getResponse().getContentAsString(), "$.items[0].id");
		MvcResult adminUsers = call(HttpMethod.GET, "/admin/users?keyword=admin@test.local", admin, null).andReturn();
		int adminId = JsonPath.read(adminUsers.getResponse().getContentAsString(), "$.items[0].id");
		call(HttpMethod.PATCH, "/admin/users/" + adminId + "/status", admin, """
				{"status":"BANNED"}""").andExpect(status().isBadRequest());
		call(HttpMethod.PATCH, "/admin/users/" + adminId + "/role", admin, """
				{"role":"USER"}""").andExpect(status().isBadRequest());
		call(HttpMethod.PATCH, "/admin/users/" + readerId + "/role", admin, """
				{"role":"ADMIN"}""").andExpect(jsonPath("$.roles", contains("ADMIN", "USER")));
		call(HttpMethod.PATCH, "/admin/users/" + readerId + "/status", admin, """
				{"status":"BANNED"}""").andExpect(jsonPath("$.status", is("BANNED")));
		call(HttpMethod.POST, "/auth/login", null, loginBody("reader@test.local", "Password@123")).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code", is("ACCOUNT_LOCKED")));
		// Token đã cấp trước khi bị khóa không còn dùng được
		call(HttpMethod.GET, "/users/me", reader, null).andExpect(status().isUnauthorized());

		// Ẩn truyện: độc giả không thấy, admin vẫn thấy; xóa mềm thì admin cũng không còn thấy
		call(HttpMethod.PATCH, "/admin/comics/" + comicId + "/visibility", admin, """
				{"hidden":true}""").andExpect(jsonPath("$.hidden", is(true)));
		call(HttpMethod.GET, "/comics/" + slug, null, null).andExpect(status().isNotFound());
		call(HttpMethod.GET, "/comics/" + slug + "/chapters/1", null, null).andExpect(status().isNotFound());
		call(HttpMethod.POST, "/chapters/" + chapterId + "/views", null, null).andExpect(status().isNotFound());
		call(HttpMethod.GET, "/admin/comics?keyword=Đấu Phá Flow", admin, null).andExpect(jsonPath("$.totalItems", is(2)));
		call(HttpMethod.PATCH, "/admin/comics/" + comicId + "/visibility", admin, """
				{"hidden":false}""").andExpect(status().isOk());
		call(HttpMethod.DELETE, "/admin/chapters/" + chapter2, admin, null).andExpect(status().isNoContent());
		call(HttpMethod.GET, "/comics/" + slug + "/chapters/1", null, null).andExpect(jsonPath("$.nextChapter").doesNotExist());
		call(HttpMethod.DELETE, "/admin/comics/" + comicId, admin, null).andExpect(status().isNoContent());
		call(HttpMethod.GET, "/comics/" + slug, null, null).andExpect(status().isNotFound());
		call(HttpMethod.GET, "/admin/comics?keyword=Đấu Phá Flow", admin, null).andExpect(jsonPath("$.totalItems", is(1)));

		// Audit log có dữ liệu trước/sau cho thao tác admin
		MvcResult audits = call(HttpMethod.GET, "/admin/audit-logs?action=COMIC_VISIBILITY", admin, null)
			.andExpect(jsonPath("$.totalItems", is(2))).andReturn();
		int auditId = JsonPath.read(audits.getResponse().getContentAsString(), "$.items[0].id");
		call(HttpMethod.GET, "/admin/audit-logs/" + auditId, admin, null).andExpect(jsonPath("$.beforeData.hidden", is(true)))
			.andExpect(jsonPath("$.afterData.hidden", is(false)));
		call(HttpMethod.GET, "/admin/audit-logs?from=2020-01-01T00:00:00Z&to=2099-01-01T00:00:00Z", admin, null).andExpect(status().isOk());
	}

	@Test
	void poisonMessageGoesToDeadLetterQueueAfterRetries() {
		rabbit.convertAndSend(RabbitConfig.NOTIFICATION_QUEUE, "khong-phai-so");
		Object dead = await().atMost(Duration.ofSeconds(30)).until(
				() -> rabbit.receiveAndConvert(RabbitConfig.DEAD_LETTER_QUEUE), message -> message != null);
		assertThat(dead).isEqualTo("khong-phai-so");
	}

	@Test
	void reusedRefreshTokenRevokesAllSessions() throws Exception {
		register("reuse", "reuse@test.local", "Reuse");
		MvcResult login = call(HttpMethod.POST, "/auth/login", null, loginBody("reuse@test.local", "Password@123")).andReturn();
		Cookie first = login.getResponse().getCookie("refresh_token");
		Cookie second = mvc.perform(request(HttpMethod.POST, API + "/auth/refresh").cookie(first)).andExpect(status().isOk())
			.andReturn().getResponse().getCookie("refresh_token");

		// Trong khoảng ân hạn (nhiều tab refresh cùng lúc): từ chối nhưng phiên mới vẫn còn
		mvc.perform(request(HttpMethod.POST, API + "/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
		Cookie third = mvc.perform(request(HttpMethod.POST, API + "/auth/refresh").cookie(second)).andExpect(status().isOk())
			.andReturn().getResponse().getCookie("refresh_token");

		// Quá khoảng ân hạn: coi là bị đánh cắp, thu hồi toàn bộ
		Thread.sleep(Duration.ofSeconds(11));
		mvc.perform(request(HttpMethod.POST, API + "/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
		mvc.perform(request(HttpMethod.POST, API + "/auth/refresh").cookie(third)).andExpect(status().isUnauthorized());
	}

	private ResultActions call(HttpMethod method, String path, String token, String body) throws Exception {
		MockHttpServletRequestBuilder builder = request(method, URI.create(API + encode(path)));
		if (token != null) {
			builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		}
		if (body != null) {
			builder.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mvc.perform(builder);
	}

	private String encode(String path) throws Exception {
		return new URI(null, null, path.split("\\?")[0], path.contains("?") ? path.split("\\?")[1] : null, null).toASCIIString();
	}

	private ResultActions upload(HttpMethod method, String path, String token, String part, byte[] bytes) throws Exception {
		return mvc.perform(multipart(method, API + path).file(new MockMultipartFile(part, "anh.png", "image/png", bytes))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private MockMultipartFile image(String part) throws IOException {
		return new MockMultipartFile(part, "anh.png", "image/png", png());
	}

	private void register(String username, String email, String fullName) throws Exception {
		call(HttpMethod.POST, "/auth/register", null, registerBody(username, email, fullName)).andExpect(status().isCreated());
	}

	private String login(String email, String password) throws Exception {
		return field(call(HttpMethod.POST, "/auth/login", null, loginBody(email, password)).andExpect(status().isOk()).andReturn(), "$.accessToken");
	}

	private String registerBody(String username, String email, String fullName) {
		return """
				{"username":"%s","email":"%s","password":"Password@123","fullName":"%s"}""".formatted(username, email, fullName);
	}

	private String loginBody(String email, String password) {
		return """
				{"email":"%s","password":"%s"}""".formatted(email, password);
	}

	private long id(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

	private String field(MvcResult result, String path) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), path);
	}

	private String tokenFromMail(String to, String subjectPrefix) {
		return await().atMost(Duration.ofSeconds(5)).until(() -> {
			ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
			verify(mailSender, timeout(5000).atLeastOnce()).send(captor.capture());
			String found = null;
			for (SimpleMailMessage message : captor.getAllValues()) {
				if (message.getTo()[0].equals(to) && message.getSubject().startsWith(subjectPrefix)) {
					Matcher matcher = TOKEN.matcher(message.getText());
					if (matcher.find()) {
						found = matcher.group(1);
					}
				}
			}
			return found;
		}, token -> token != null);
	}

	private byte[] png() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(4, 6, BufferedImage.TYPE_INT_RGB), "png", out);
		return out.toByteArray();
	}

	private int httpStatus(String url) throws Exception {
		try (HttpClient client = HttpClient.newHttpClient()) {
			return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode();
		}
	}

}
