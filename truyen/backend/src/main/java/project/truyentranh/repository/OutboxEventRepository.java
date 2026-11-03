package project.truyentranh.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

	List<OutboxEvent> findTop100BySentAtIsNullOrderByIdAsc();

	@Modifying
	@Query("delete from OutboxEvent e where e.sentAt < :before")
	int deleteSentBefore(@Param("before") Instant before);

}
