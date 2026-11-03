package project.truyentranh;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/** Backend đọc được dữ liệu mẫu trong database/02_seed.sql, đúng như khi chạy trên MySQL Workbench. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SeedTestcontainersConfiguration.class)
class SeedDataTests {

	private static final String API = "/api/v1";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private JavaMailSender mailSender;

	@Test
	void seededAccountsCanLogInExceptBannedOne() throws Exception {
		String admin = login("admin@comicweb.example", "Admin@12345");
		mvc.perform(get(API + "/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
			.andExpect(jsonPath("$.totalItems", is(5)));
		mvc.perform(get(API + "/admin/dashboard/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
			.andExpect(jsonPath("$.comics", is(12)))
			.andExpect(jsonPath("$.pendingReports", is(2)));

		String reader = login("minhan@comicweb.example", "Password@123");
		mvc.perform(get(API + "/me/follows").header(HttpHeaders.AUTHORIZATION, "Bearer " + reader))
			.andExpect(jsonPath("$.items", hasSize(3)));
		mvc.perform(get(API + "/me/history").header(HttpHeaders.AUTHORIZATION, "Bearer " + reader))
			.andExpect(jsonPath("$.items", hasSize(2)));
		mvc.perform(get(API + "/me/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, "Bearer " + reader))
			.andExpect(jsonPath("$.count", is(3)));

		mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(loginBody("spammer@comicweb.example", "Password@123")))
			.andExpect(status().isForbidden());
	}

	@Test
	void seededComicsAreReadable() throws Exception {
		mvc.perform(get(API + "/comics?size=50"))
			.andExpect(jsonPath("$.totalItems", is(11)))
			.andExpect(jsonPath("$.items[*].slug", not(hasItem("ban-thao-chua-duyet"))));
		mvc.perform(get(API + "/comics?genre=trinh-tham")).andExpect(jsonPath("$.totalItems", is(1)));
		mvc.perform(get(API + "/comics?keyword=lam vu")).andExpect(jsonPath("$.totalItems", is(3)));
		mvc.perform(get(API + "/comics/hanh-trinh-den-dao-may"))
			.andExpect(jsonPath("$.authorName", is("Lam Vũ")))
			.andExpect(jsonPath("$.ratingCount", is(3)));
		mvc.perform(get(API + "/comics/hanh-trinh-den-dao-may/chapters")).andExpect(jsonPath("$", hasSize(7)));
		mvc.perform(get(API + "/comics/hanh-trinh-den-dao-may/chapters/2.5"))
			.andExpect(jsonPath("$.pages", hasSize(5)))
			.andExpect(jsonPath("$.prevChapter", is(2.0)))
			.andExpect(jsonPath("$.nextChapter", is(3.0)))
			.andExpect(jsonPath("$.pages[0].imageUrl", is("https://placehold.co/800x1200/png?text=C1-2.5-P1")));
		mvc.perform(get(API + "/comics/ban-thao-chua-duyet")).andExpect(status().isNotFound());
		mvc.perform(get(API + "/comics/1/comments")).andExpect(jsonPath("$.items", hasSize(2)));
		mvc.perform(get(API + "/comics/hot?period=week")).andExpect(jsonPath("$[0].slug", is("hanh-trinh-den-dao-may")));
		mvc.perform(get(API + "/authors")).andExpect(jsonPath("$", hasSize(5)));
		mvc.perform(get(API + "/genres")).andExpect(jsonPath("$", hasSize(8)));
	}

	private String login(String email, String password) throws Exception {
		String body = mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	private String loginBody(String email, String password) {
		return """
				{"email":"%s","password":"%s"}""".formatted(email, password);
	}

}
