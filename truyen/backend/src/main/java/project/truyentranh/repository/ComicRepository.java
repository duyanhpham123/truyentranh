package project.truyentranh.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.Comic;
import project.truyentranh.model.ComicStatus;

public interface ComicRepository extends JpaRepository<Comic, Long> {

	@Query("select c from Comic c where c.slug = :slug and c.hidden = false and c.deletedAt is null")
	Optional<Comic> findVisibleBySlug(@Param("slug") String slug);

	@Query("select c from Comic c where c.id = :id and c.hidden = false and c.deletedAt is null")
	Optional<Comic> findVisibleById(@Param("id") long id);

	@Query("select c from Comic c where c.id = :id and c.deletedAt is null")
	Optional<Comic> findActiveById(@Param("id") long id);

	@Query("select c from Comic c where c.id in :ids and c.hidden = false and c.deletedAt is null")
	List<Comic> findVisibleByIds(@Param("ids") Collection<Long> ids);

	boolean existsBySlug(String slug);

	boolean existsByAuthorId(Long authorId);

	boolean existsByCoverUrl(String coverUrl);

	@Query("""
			select c from Comic c left join c.author a
			where c.hidden = false and c.deletedAt is null
			  and (:keyword is null or c.title like concat('%', :keyword, '%') escape '\\'
			       or a.name like concat('%', :keyword, '%') escape '\\')
			  and (:status is null or c.status = :status)
			  and (:authorId is null or a.id = :authorId)
			  and (:genre is null or exists (select 1 from c.genres g where g.slug = :genre))
			""")
	Page<Comic> search(@Param("keyword") String keyword, @Param("genre") String genre,
			@Param("status") ComicStatus status, @Param("authorId") Long authorId, Pageable pageable);

	@Query("""
			select c from Comic c
			where c.deletedAt is null
			  and (:keyword is null or c.title like concat('%', :keyword, '%') escape '\\')
			""")
	Page<Comic> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);

	@Query("select c from Comic c where c.hidden = false and c.deletedAt is null order by c.views desc")
	List<Comic> findMostViewed(Pageable pageable);

	@Modifying(clearAutomatically = true)
	@Query(value = """
			update comics c set
			  rating_avg = coalesce((select avg(r.score) from ratings r where r.comic_id = c.id), 0),
			  rating_count = (select count(*) from ratings r where r.comic_id = c.id)
			where c.id = :id
			""", nativeQuery = true)
	void recalculateRating(@Param("id") long id);

}
