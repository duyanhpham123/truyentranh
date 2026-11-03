package project.truyentranh.util;

import java.awt.Dimension;
import java.nio.charset.StandardCharsets;

/** Đọc kích thước từ header WEBP, vì Java không có sẵn reader cho định dạng này. */
public final class WebpUtil {

	private WebpUtil() {
	}

	/** Trả về null nếu header không đúng định dạng. */
	public static Dimension readSize(byte[] b) {
		if (b.length < 30) {
			return null;
		}
		String chunk = new String(b, 12, 4, StandardCharsets.US_ASCII);
		return switch (chunk) {
			case "VP8 " -> new Dimension(le16(b, 26) & 0x3FFF, le16(b, 28) & 0x3FFF);
			case "VP8L" -> {
				int bits = (b[21] & 0xFF) | (b[22] & 0xFF) << 8 | (b[23] & 0xFF) << 16 | (b[24] & 0xFF) << 24;
				yield new Dimension((bits & 0x3FFF) + 1, ((bits >> 14) & 0x3FFF) + 1);
			}
			case "VP8X" -> new Dimension(le24(b, 24) + 1, le24(b, 27) + 1);
			default -> null;
		};
	}

	private static int le16(byte[] b, int i) {
		return (b[i] & 0xFF) | (b[i + 1] & 0xFF) << 8;
	}

	private static int le24(byte[] b, int i) {
		return le16(b, i) | (b[i + 2] & 0xFF) << 16;
	}

}
