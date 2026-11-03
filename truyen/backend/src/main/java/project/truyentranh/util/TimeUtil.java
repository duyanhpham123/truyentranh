package project.truyentranh.util;

import java.time.LocalDate;
import java.time.ZoneId;

public final class TimeUtil {

	/** Múi giờ dùng để chốt "một ngày" trong thống kê view và dashboard. DB luôn lưu UTC. */
	public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

	public static final String SQL_OFFSET = "+07:00";

	private TimeUtil() {
	}

	public static LocalDate today() {
		return LocalDate.now(ZONE);
	}

}
