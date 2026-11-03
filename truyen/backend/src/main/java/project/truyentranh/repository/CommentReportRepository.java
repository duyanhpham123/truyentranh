package project.truyentranh.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.truyentranh.model.CommentReport;
import project.truyentranh.model.ReportStatus;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

	boolean existsByCommentIdAndReporterId(long commentId, long reporterId);

	@Query(value = """
			select r from CommentReport r join fetch r.comment c join fetch r.reporter
			where r.status = :status order by r.createdAt desc, r.id desc
			""", countQuery = "select count(r) from CommentReport r where r.status = :status")
	Page<CommentReport> findByStatus(@Param("status") ReportStatus status, Pageable pageable);

	@Modifying
	@Query("""
			update CommentReport r set r.status = project.truyentranh.model.ReportStatus.RESOLVED
			where r.comment.id = :commentId and r.status = project.truyentranh.model.ReportStatus.PENDING
			""")
	void resolveAllOf(@Param("commentId") long commentId);

}
