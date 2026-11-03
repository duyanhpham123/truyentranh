package project.truyentranh.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class WebpUtilTest {

	@Test
	void readsLossySize() {
		byte[] b = header("VP8 ");
		put16(b, 26, 800);
		put16(b, 28, 1200);
		assertThat(WebpUtil.readSize(b)).isEqualTo(new Dimension(800, 1200));
	}

	@Test
	void readsLosslessSize() {
		byte[] b = header("VP8L");
		int bits = (800 - 1) | (1200 - 1) << 14;
		b[21] = (byte) bits;
		b[22] = (byte) (bits >> 8);
		b[23] = (byte) (bits >> 16);
		b[24] = (byte) (bits >> 24);
		assertThat(WebpUtil.readSize(b)).isEqualTo(new Dimension(800, 1200));
	}

	@Test
	void readsExtendedSize() {
		byte[] b = header("VP8X");
		put16(b, 24, 800 - 1);
		put16(b, 27, 1200 - 1);
		assertThat(WebpUtil.readSize(b)).isEqualTo(new Dimension(800, 1200));
	}

	@Test
	void returnsNullForUnknownOrShortHeader() {
		assertThat(WebpUtil.readSize(header("ABCD"))).isNull();
		assertThat(WebpUtil.readSize(new byte[10])).isNull();
	}

	private byte[] header(String chunk) {
		byte[] b = new byte[32];
		System.arraycopy(chunk.getBytes(StandardCharsets.US_ASCII), 0, b, 12, 4);
		return b;
	}

	private void put16(byte[] b, int i, int value) {
		b[i] = (byte) value;
		b[i + 1] = (byte) (value >> 8);
	}

}
