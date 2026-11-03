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

import project.truyentranh.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@Query(value = """
			select c from Comment c join fetch c.user
			where c.comicId = :comicId and c.parentId is null
			  and c.status = project.truyentranh.model.CommentStatus.VISIBLE
			  and ((:chapterId is null and c.chapterId is null) or c.chapterId = :chapterId)
			order by c.createdAt desc, c.id desc
			""", countQuery = """
			select count(c) from Comment c
			where c.comicId = :comicId and c.parentId is null
			  and c.status = project.truyentranh.model.CommentStatus.VISIBLE
			  and ((:chapterId is null and c.chapterId is null) or c.chapterId = :chapterId)
			""")
	Page<Comment> findRoots(@Param("comicId") long comicId, @Param("chapterId") Long chapterId, Pageable pageable);

	@Query("""
			select c from Comment c join fetch c.user
			where c.parentId in :parentIds and c.status = project.truyentranh.model.CommentStatus.VISIBLE
			order by c.createdAt asc, c.id asc
			""")
	List<Comment> findReplies(@Param("parentIds") Collection<Long> parentIds);

	@Query("select c from Comment c where c.id = :id and c.status = project.truyentranh.model.CommentStatus.VISIBLE")
	Optional<Comment> findVisibleById(@Param("id") long id);

	@Modifying
	@Query(value = "insert ignore into comment_likes (comment_id, user_id, created_at) values (:commentId, :userId, utc_timestamp())", nativeQuery = true)
	int addLike(@Param("commentId") long commentId, @Param("userId") long userId);

	@Modifying
	@Query(value = "delete from comment_likes where comment_id = :commentId and user_id = :userId", nativeQuery = true)
	int removeLike(@Param("commentId") long commentId, @Param("userId") long userId);

	@Modifying
	@Query("update Comment c set c.likeCount = c.likeCount + :delta where c.id = :id")
	void changeLikeCount(@Param("id") long id, @Param("delta") int delta);

}
