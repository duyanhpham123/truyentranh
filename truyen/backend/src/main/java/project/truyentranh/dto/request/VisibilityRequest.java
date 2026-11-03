package project.truyentranh.dto.request;

import jakarta.validation.constraints.NotNull;

public record VisibilityRequest(@NotNull Boolean hidden) {

}
