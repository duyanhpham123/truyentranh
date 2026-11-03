package project.truyentranh.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRoleRequest(@NotBlank @Pattern(regexp = "USER|ADMIN") String role) {

}
