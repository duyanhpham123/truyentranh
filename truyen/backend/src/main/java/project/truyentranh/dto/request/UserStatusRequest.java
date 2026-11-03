package project.truyentranh.dto.request;

import jakarta.validation.constraints.NotNull;
import project.truyentranh.model.UserStatus;

public record UserStatusRequest(@NotNull UserStatus status) {

}
