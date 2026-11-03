package project.truyentranh.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long actorId;

	private String action;

	private String targetType;

	private Long targetId;

	@Column(columnDefinition = "json")
	private String beforeData;

	@Column(columnDefinition = "json")
	private String afterData;

	@CreationTimestamp
	private Instant createdAt;

}
