package project.truyentranh.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {

	private SlugUtil() {
	}

	/** "Đấu Phá Thương Khung" -> "dau-pha-thuong-khung" */
	public static String toSlug(String text) {
		String slug = Normalizer.normalize(text.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", "-")
			.replaceAll("(^-|-$)", "");
		return slug.isEmpty() ? "n-a" : slug;
	}

}
