package project.truyentranh.mapper;

import org.springframework.stereotype.Component;

import project.truyentranh.dto.response.AuditDetailResponse;
import project.truyentranh.dto.response.AuditSummaryResponse;
import project.truyentranh.model.AuditLog;

@Component
public class AuditMapper {

	public AuditSummaryResponse toSummary(AuditLog a) {
		return new AuditSummaryResponse(a.getId(), a.getActorId(), a.getAction(), a.getTargetType(), a.getTargetId(), a.getCreatedAt());
	}

	public AuditDetailResponse toDetail(AuditLog a) {
		return new AuditDetailResponse(a.getId(), a.getActorId(), a.getAction(), a.getTargetType(), a.getTargetId(),
				a.getBeforeData(), a.getAfterData(), a.getCreatedAt());
	}

}
