package project.truyentranh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.ChapterPage;

public interface ChapterPageRepository extends JpaRepository<ChapterPage, Long> {

	List<ChapterPage> findByChapterIdOrderByPageNumberAsc(long chapterId);

	boolean existsByChapterId(long chapterId);

	boolean existsByImageUrl(String imageUrl);

	@Query("select coalesce(max(p.pageNumber), 0) from ChapterPage p where p.chapterId = :chapterId")
	int findMaxPageNumber(@Param("chapterId") long chapterId);

	@Modifying
	@Query("delete from ChapterPage p where p.chapterId = :chapterId")
	void deleteByChapterId(@Param("chapterId") long chapterId);

}
