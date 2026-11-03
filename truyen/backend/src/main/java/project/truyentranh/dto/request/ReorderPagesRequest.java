package project.truyentranh.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public record ReorderPagesRequest(@NotEmpty List<Long> pageIds) {

}
