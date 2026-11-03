package project.truyentranh.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.truyentranh.dto.response.DailyCountResponse;
import project.truyentranh.dto.response.DashboardSummaryResponse;
import project.truyentranh.dto.response.TopComicResponse;
import project.truyentranh.dto.response.TopGenreResponse;
import project.truyentranh.exception.ApiException;
import project.truyentranh.repository.DashboardRepository;
import project.truyentranh.util.TimeUtil;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

	private final DashboardRepository repository;

	public DashboardSummaryResponse summary() {
		return repository.summary(TimeUtil.today());
	}

	public List<TopComicResponse> topComics(LocalDate from, LocalDate to, int limit) {
		return repository.topComics(fromOrDefault(from), toOrDefault(to), checkLimit(limit));
	}

	public List<DailyCountResponse> views(LocalDate from, LocalDate to) {
		return repository.viewsByDay(fromOrDefault(from), toOrDefault(to));
	}

	public List<DailyCountResponse> newUsers(LocalDate from, LocalDate to) {
		return repository.newUsersByDay(fromOrDefault(from), toOrDefault(to));
	}

	public List<TopGenreResponse> topGenres(LocalDate from, LocalDate to, int limit) {
		return repository.topGenres(fromOrDefault(from), toOrDefault(to), checkLimit(limit));
	}

	private LocalDate fromOrDefault(LocalDate from) {
		return from != null ? from : TimeUtil.today().minusDays(29);
	}

	private LocalDate toOrDefault(LocalDate to) {
		return to != null ? to : TimeUtil.today();
	}

	private int checkLimit(int limit) {
		if (limit < 1 || limit > 100) {
			throw ApiException.badRequest("INVALID_LIMIT", "limit phải từ 1 đến 100");
		}
		return limit;
	}

}
