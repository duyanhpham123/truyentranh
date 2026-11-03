package project.truyentranh.dto.request;

import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import project.truyentranh.model.ComicStatus;

public record ComicRequest(@NotBlank @Size(max = 255) String title, Long authorId, String description, @NotNull ComicStatus status, Set<Long> genreIds) {

}
