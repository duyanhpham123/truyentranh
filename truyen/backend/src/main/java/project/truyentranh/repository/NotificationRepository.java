package project.truyentranh.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/**
	 * Tạo thông báo cho toàn bộ người theo dõi bằng một câu lệnh, không phải nạp danh sách vào bộ nhớ.
	 * INSERT IGNORE cùng unique key làm cho việc xử lý lại cùng một event không tạo trùng.
	 */
	@Modifying
	@Query(value = """
			insert ignore into notifications (user_id, chapter_id, type, message, created_at)
			select f.user_id, :chapterId, :type, :message, utc_timestamp() from favorites f where f.comic_id = :comicId
			""", nativeQuery = true)
	int createForFollowers(@Param("chapterId") long chapterId, @Param("type") String type,
			@Param("message") String message, @Param("comicId") long comicId);

	@Query(value = """
			select n from Notification n join fetch n.chapter ch join fetch ch.comic
			where n.userId = :userId order by n.id desc
			""", countQuery = "select count(n) from Notification n where n.userId = :userId")
	Page<Notification> findForUser(@Param("userId") long userId, Pageable pageable);

	long countByUserIdAndReadAtIsNull(long userId);

	boolean existsByIdAndUserId(long id, long userId);

	@Modifying
	@Query(value = "update notifications set read_at = utc_timestamp() where id = :id and read_at is null", nativeQuery = true)
	void markRead(@Param("id") long id);

	@Modifying
	@Query(value = "update notifications set read_at = utc_timestamp() where user_id = :userId and read_at is null", nativeQuery = true)
	void markAllRead(@Param("userId") long userId);

}
