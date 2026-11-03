package project.truyentranh.dto.response;

import java.time.LocalDate;

public record DailyCountResponse(LocalDate date, long count) {

}
