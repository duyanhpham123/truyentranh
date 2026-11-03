package project.truyentranh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.Genre;

public interface GenreRepository extends JpaRepository<Genre, Long> {

	Optional<Genre> findBySlug(String slug);

	boolean existsBySlug(String slug);

	List<Genre> findAllByOrderByNameAsc();

	@Query(value = "select count(*) from comic_genres where genre_id = :id", nativeQuery = true)
	long countComics(@Param("id") long id);

}
