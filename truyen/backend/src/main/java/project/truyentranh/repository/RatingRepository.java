package project.truyentranh.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {

	Optional<Rating> findByUserIdAndComicId(long userId, long comicId);

	@Modifying
	@Query(value = """
			insert into ratings (user_id, comic_id, score, created_at, updated_at)
			values (:userId, :comicId, :score, utc_timestamp(), utc_timestamp())
			on duplicate key update score = values(score), updated_at = values(updated_at)
			""", nativeQuery = true)
	void upsert(@Param("userId") long userId, @Param("comicId") long comicId, @Param("score") int score);

}
