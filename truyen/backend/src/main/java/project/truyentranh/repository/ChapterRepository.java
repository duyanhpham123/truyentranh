package project.truyentranh.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.ComicChapter;

public interface ChapterRepository extends JpaRepository<ComicChapter, Long> {

	/** Một dòng kết quả: id truyện và số chapter mới nhất. */
	interface LatestNumber {

		Long getComicId();

		BigDecimal getNumber();

	}

	@Query("""
			select c.comic.id as comicId, max(c.chapterNumber) as number
			from ComicChapter c
			where c.comic.id in :ids and c.status = project.truyentranh.model.ChapterStatus.PUBLISHED and c.deletedAt is null
			group by c.comic.id
			""")
	List<LatestNumber> findLatestNumbers(@Param("ids") Collection<Long> ids);

	@Query("""
			select c from ComicChapter c
			where c.comic.id = :comicId and c.status = project.truyentranh.model.ChapterStatus.PUBLISHED and c.deletedAt is null
			order by c.chapterNumber desc
			""")
	List<ComicChapter> findPublished(@Param("comicId") long comicId);

	@Query("""
			select c from ComicChapter c
			where c.comic.id = :comicId and c.chapterNumber = :number
			  and c.status = project.truyentranh.model.ChapterStatus.PUBLISHED and c.deletedAt is null
			""")
	Optional<ComicChapter> findPublishedByNumber(@Param("comicId") long comicId, @Param("number") BigDecimal number);

	@Query("""
			select c from ComicChapter c
			where c.id = :id and c.comic.id = :comicId
			  and c.status = project.truyentranh.model.ChapterStatus.PUBLISHED and c.deletedAt is null
			""")
	Optional<ComicChapter> findPublishedInComic(@Param("id") long id, @Param("comicId") long comicId);

	@Query("""
			select c.chapterNumber from ComicChapter c
			where c.comic.id = :comicId and c.chapterNumber < :number
			  and c.status = project.truyentranh.model.ChapterStatus.PUBLISHED and c.deletedAt is null
			order by c.chapterNumber desc
			""")
	List<BigDecimal> findPreviousNumbers(@Param("comicId") long comicId, @Param("number") BigDecimal number, Pageable pageable);

	@Query("""
			select c.chapterNumber from ComicChapter c
			where c.comic.id = :comicId and c.chapterNumber > :number
			  and c.status = project.truyentranh.model.ChapterStatus.PUBLISHED and c.deletedAt is null
			order by c.chapterNumber asc
			""")
	List<BigDecimal> findNextNumbers(@Param("comicId") long comicId, @Param("number") BigDecimal number, Pageable pageable);

	@Query("select c from ComicChapter c where c.id = :id and c.deletedAt is null")
	Optional<ComicChapter> findActiveById(@Param("id") long id);

	@Query("select c from ComicChapter c where c.comic.id = :comicId and c.deletedAt is null order by c.chapterNumber desc")
	List<ComicChapter> findAllActive(@Param("comicId") long comicId);

	@Query("""
			select max(c.publishedAt) from ComicChapter c
			where c.comic.id = :comicId and c.status = project.truyentranh.model.ChapterStatus.PUBLISHED and c.deletedAt is null
			""")
	Instant findLastPublishedAt(@Param("comicId") long comicId);

	/** Chapter đã publish của truyện đang hiển thị; dùng khi ghi view và gửi thông báo. */
	@Query("""
			select c from ComicChapter c join fetch c.comic comic
			where c.id = :id and c.status = project.truyentranh.model.ChapterStatus.PUBLISHED and c.deletedAt is null
			  and comic.hidden = false and comic.deletedAt is null
			""")
	Optional<ComicChapter> findReadable(@Param("id") long id);

}
