package project.truyentranh.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugUtilTest {

	@Test
	void removesVietnameseAccents() {
		assertThat(SlugUtil.toSlug("Đấu Phá Thương Khung")).isEqualTo("dau-pha-thuong-khung");
		assertThat(SlugUtil.toSlug("  Tập 1: Mở đầu!! ")).isEqualTo("tap-1-mo-dau");
	}

	@Test
	void fallsBackWhenNothingRemains() {
		assertThat(SlugUtil.toSlug("!!!")).isEqualTo("n-a");
	}

}
