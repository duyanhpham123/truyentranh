package project.truyentranh.service;

import java.time.Instant;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.truyentranh.util.PageUtil;
import project.truyentranh.dto.response.AuditDetailResponse;
import project.truyentranh.dto.response.AuditSummaryResponse;
import project.truyentranh.dto.response.PagedResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.mapper.AuditMapper;
import project.truyentranh.model.AuditLog;
import project.truyentranh.repository.AuditLogRepository;
import project.truyentranh.security.CurrentUser;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class AuditService {

	private final AuditLogRepository repository;

	private final AuditMapper mapper;

	private final JsonMapper json;

	/** Gọi trong cùng transaction với thao tác của admin, thao tác lỗi thì không có log. */
	@Transactional
	public void record(String action, String targetType, Long targetId, Object before, Object after) {
		AuditLog log = new AuditLog();
		log.setActorId(CurrentUser.id());
		log.setAction(action);
		log.setTargetType(targetType);
		log.setTargetId(targetId);
		log.setBeforeData(before == null ? null : json.writeValueAsString(before));
		log.setAfterData(after == null ? null : json.writeValueAsString(after));
		repository.save(log);
	}

	@Transactional(readOnly = true)
	public PagedResponse<AuditSummaryResponse> search(Long actor, String action, Instant from, Instant to, int page, int size) {
		return PagedResponse.of(repository.search(actor, action, from, to, PageUtil.of(page, size, Sort.by(Sort.Direction.DESC, "id"))),
				mapper::toSummary);
	}

	@Transactional(readOnly = true)
	public AuditDetailResponse detail(long id) {
		return mapper.toDetail(repository.findById(id).orElseThrow(() -> ApiException.notFound("audit log")));
	}

}
