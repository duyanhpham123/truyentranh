package project.truyentranh.dto.response;

import java.math.BigDecimal;

public record LastReadResponse(Long chapterId, BigDecimal chapterNumber, int page) {

}
