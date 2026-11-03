package project.truyentranh.repository;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/** Ghi lượt xem gom theo lô từ Redis xuống MySQL. */
@Repository
@RequiredArgsConstructor
public class ViewStatRepository {

	private final JdbcClient jdbc;

	public void addComicViews(long comicId, long count, LocalDate date) {
		jdbc.sql("update comics set views = views + :n where id = :id").param("n", count).param("id", comicId).update();
		jdbc.sql("""
				insert into comic_view_daily (comic_id, view_date, view_count) values (:id, :date, :n)
				on duplicate key update view_count = view_count + values(view_count)
				""")
			.param("id", comicId).param("date", date).param("n", count).update();
	}

	public void addChapterViews(long chapterId, long count) {
		jdbc.sql("update comic_chapters set views = views + :n where id = :id").param("n", count).param("id", chapterId).update();
	}

}
