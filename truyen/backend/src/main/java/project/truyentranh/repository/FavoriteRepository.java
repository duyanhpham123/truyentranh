package project.truyentranh.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.Favorite;
import project.truyentranh.model.FavoriteId;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

	boolean existsByUserIdAndComicId(long userId, long comicId);

	@Modifying
	@Query(value = "insert ignore into favorites (user_id, comic_id, created_at) values (:userId, :comicId, utc_timestamp())", nativeQuery = true)
	void addIfAbsent(@Param("userId") long userId, @Param("comicId") long comicId);

	@Modifying
	@Query("delete from Favorite f where f.userId = :userId and f.comicId = :comicId")
	void remove(@Param("userId") long userId, @Param("comicId") long comicId);

	@Query(value = """
			select f from Favorite f join fetch f.comic c
			where f.userId = :userId and c.hidden = false and c.deletedAt is null
			order by c.lastChapterAt desc, f.createdAt desc
			""", countQuery = """
			select count(f) from Favorite f join f.comic c
			where f.userId = :userId and c.hidden = false and c.deletedAt is null
			""")
	Page<Favorite> findVisible(@Param("userId") long userId, Pageable pageable);

}
