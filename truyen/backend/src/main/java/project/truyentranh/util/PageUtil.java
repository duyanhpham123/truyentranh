package project.truyentranh.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageUtil {

	private static final int MAX_SIZE = 100;

	private PageUtil() {
	}

	/** Chặn giá trị page/size bất thường từ query string. */
	public static PageRequest of(int page, int size) {
		return PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, MAX_SIZE)));
	}

	public static PageRequest of(int page, int size, Sort sort) {
		return of(page, size).withSort(sort);
	}

}
