package project.truyentranh.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChapterRequest(@NotNull @Positive @Digits(integer = 6, fraction = 2) BigDecimal chapterNumber, @Size(max = 255) String chapterTitle) {

}
