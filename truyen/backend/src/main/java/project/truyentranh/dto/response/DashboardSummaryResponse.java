package project.truyentranh.dto.response;

public record DashboardSummaryResponse(long users, long comics, long chapters, long totalViews, long viewsToday, long newUsersToday, long pendingReports) {

}
