package project.truyentranh.dto.request;

import jakarta.validation.constraints.NotNull;
import project.truyentranh.model.ReportStatus;

public record ResolveReportRequest(@NotNull ReportStatus status) {

}
