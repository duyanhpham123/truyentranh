package project.truyentranh.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	@Query("""
			select a from AuditLog a
			where (:actor is null or a.actorId = :actor)
			  and (:action is null or a.action = :action)
			  and (cast(:from as Instant) is null or a.createdAt >= :from)
			  and (cast(:to as Instant) is null or a.createdAt < :to)
			""")
	Page<AuditLog> search(@Param("actor") Long actor, @Param("action") String action, @Param("from") Instant from,
			@Param("to") Instant to, Pageable pageable);

}
