package project.truyentranh.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.DailyCountResponse;
import project.truyentranh.dto.response.DashboardSummaryResponse;
import project.truyentranh.dto.response.TopComicResponse;
import project.truyentranh.dto.response.TopGenreResponse;
import project.truyentranh.util.TimeUtil;

/** Truy vấn thống kê tổng hợp nhiều bảng nên dùng SQL trực tiếp. */
@Repository
@RequiredArgsConstructor
public class DashboardRepository {

	private final JdbcClient jdbc;

	public DashboardSummaryResponse summary(LocalDate today) {
		return jdbc.sql("""
				select
				  (select count(*) from users) users,
				  (select count(*) from comics where deleted_at is null) comics,
				  (select count(*) from comic_chapters where status = 'PUBLISHED' and deleted_at is null) chapters,
				  (select coalesce(sum(views), 0) from comics where deleted_at is null) total_views,
				  (select coalesce(sum(view_count), 0) from comic_view_daily where view_date = :today) views_today,
				  (select count(*) from users where date(convert_tz(created_at, '+00:00', :offset)) = :today) new_users_today,
				  (select count(*) from comment_reports where status = 'PENDING') pending_reports
				""")
			.param("today", today).param("offset", TimeUtil.SQL_OFFSET)
			.query((rs, i) -> new DashboardSummaryResponse(rs.getLong("users"), rs.getLong("comics"), rs.getLong("chapters"),
					rs.getLong("total_views"), rs.getLong("views_today"), rs.getLong("new_users_today"),
					rs.getLong("pending_reports")))
			.single();
	}

	public List<TopComicResponse> topComics(LocalDate from, LocalDate to, int limit) {
		return jdbc.sql("""
				select c.id, c.slug, c.title, sum(d.view_count) total
				from comic_view_daily d
				join comics c on c.id = d.comic_id and c.deleted_at is null
				where d.view_date between :from and :to
				group by c.id, c.slug, c.title
				order by total desc limit :limit
				""")
			.param("from", from).param("to", to).param("limit", limit)
			.query((rs, i) -> new TopComicResponse(rs.getLong("id"), rs.getString("slug"), rs.getString("title"), rs.getLong("total")))
			.list();
	}

	public List<DailyCountResponse> viewsByDay(LocalDate from, LocalDate to) {
		return jdbc.sql("""
				select view_date day, sum(view_count) total from comic_view_daily
				where view_date between :from and :to
				group by view_date order by view_date
				""")
			.param("from", from).param("to", to)
			.query((rs, i) -> new DailyCountResponse(rs.getDate("day").toLocalDate(), rs.getLong("total")))
			.list();
	}

	public List<DailyCountResponse> newUsersByDay(LocalDate from, LocalDate to) {
		return jdbc.sql("""
				select date(convert_tz(created_at, '+00:00', :offset)) day, count(*) total from users
				where date(convert_tz(created_at, '+00:00', :offset)) between :from and :to
				group by day order by day
				""")
			.param("offset", TimeUtil.SQL_OFFSET).param("from", from).param("to", to)
			.query((rs, i) -> new DailyCountResponse(rs.getDate("day").toLocalDate(), rs.getLong("total")))
			.list();
	}

	public List<TopGenreResponse> topGenres(LocalDate from, LocalDate to, int limit) {
		return jdbc.sql("""
				select g.id, g.name, sum(d.view_count) total
				from comic_view_daily d
				join comic_genres cg on cg.comic_id = d.comic_id
				join genres g on g.id = cg.genre_id
				where d.view_date between :from and :to
				group by g.id, g.name
				order by total desc limit :limit
				""")
			.param("from", from).param("to", to).param("limit", limit)
			.query((rs, i) -> new TopGenreResponse(rs.getLong("id"), rs.getString("name"), rs.getLong("total")))
			.list();
	}

}
