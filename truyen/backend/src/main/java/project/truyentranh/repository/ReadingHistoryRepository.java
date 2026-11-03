package project.truyentranh.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.ReadingHistory;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {

	@Query("""
			select h from ReadingHistory h join fetch h.chapter
			where h.userId = :userId and h.comic.id = :comicId
			""")
	Optional<ReadingHistory> findOne(@Param("userId") long userId, @Param("comicId") long comicId);

	@Modifying
	@Query(value = """
			insert into reading_histories (user_id, comic_id, chapter_id, last_page, last_read_at)
			values (:userId, :comicId, :chapterId, :page, utc_timestamp())
			on duplicate key update chapter_id = values(chapter_id), last_page = values(last_page), last_read_at = values(last_read_at)
			""", nativeQuery = true)
	void upsert(@Param("userId") long userId, @Param("comicId") long comicId, @Param("chapterId") long chapterId, @Param("page") int page);

	@Query(value = """
			select h from ReadingHistory h join fetch h.comic c join fetch h.chapter
			where h.userId = :userId and c.hidden = false and c.deletedAt is null
			order by h.lastReadAt desc, h.id desc
			""", countQuery = """
			select count(h) from ReadingHistory h join h.comic c
			where h.userId = :userId and c.hidden = false and c.deletedAt is null
			""")
	Page<ReadingHistory> findVisible(@Param("userId") long userId, Pageable pageable);

	@Modifying
	@Query("delete from ReadingHistory h where h.userId = :userId and h.comic.id = :comicId")
	void remove(@Param("userId") long userId, @Param("comicId") long comicId);

}
