package project.truyentranh.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProgressRequest(@NotNull Long chapterId, @Min(1) int page) {

}
