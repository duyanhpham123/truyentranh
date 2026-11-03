package project.truyentranh.service;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Predicate;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import project.truyentranh.config.AppProperties;
import project.truyentranh.exception.ApiException;
import project.truyentranh.util.WebpUtil;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/** Lưu ảnh lên MinIO/S3. Các cột *_url trong DB giữ object key, hoặc URL đầy đủ với ảnh ở nơi khác. */
@Slf4j
@Service
public class StorageService {

	private final S3Client s3;

	private final String bucket;

	private final String publicBaseUrl;

	public StorageService(AppProperties props) {
		AppProperties.Storage config = props.storage();
		this.bucket = config.bucket();
		this.publicBaseUrl = config.publicBaseUrl().replaceAll("/$", "");
		this.s3 = S3Client.builder()
			.endpointOverride(URI.create(config.endpoint()))
			.region(Region.of(config.region()))
			.credentialsProvider(StaticCredentialsProvider
				.create(AwsBasicCredentials.create(config.accessKey(), config.secretKey())))
			.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
			.build();
	}

	public record StoredImage(String key, Integer width, Integer height) {
	}

	@EventListener(ApplicationReadyEvent.class)
	void ensureBucket() {
		try {
			try {
				s3.headBucket(b -> b.bucket(bucket));
			}
			catch (S3Exception ex) {
				s3.createBucket(b -> b.bucket(bucket));
				log.info("Đã tạo bucket {}", bucket);
			}
			String policy = """
					{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*","Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}"""
				.formatted(bucket);
			s3.putBucketPolicy(b -> b.bucket(bucket).policy(policy));
		}
		catch (RuntimeException ex) {
			log.warn("Không khởi tạo được bucket {}: {}", bucket, ex.getMessage());
		}
	}

	public StoredImage store(String prefix, MultipartFile file) {
		byte[] bytes;
		try {
			bytes = file.getBytes();
		}
		catch (IOException ex) {
			throw ApiException.badRequest("UPLOAD_FAILED", "Không đọc được file tải lên");
		}
		ImageType type = ImageType.detect(bytes);
		if (type == null) {
			throw ApiException.badRequest("INVALID_IMAGE", "Chỉ nhận ảnh JPEG, PNG hoặc WEBP");
		}
		Dimension size = type == ImageType.WEBP ? WebpUtil.readSize(bytes) : readSize(bytes);
		String key = prefix + "/" + UUID.randomUUID() + "." + type.extension;
		s3.putObject(b -> b.bucket(bucket).key(key).contentType(type.contentType).cacheControl("public, max-age=31536000, immutable"),
				RequestBody.fromBytes(bytes));
		return new StoredImage(key, size == null ? null : size.width, size == null ? null : size.height);
	}

	public String url(String keyOrUrl) {
		if (keyOrUrl == null || isExternal(keyOrUrl)) {
			return keyOrUrl;
		}
		return publicBaseUrl + "/" + keyOrUrl;
	}

	public void deleteQuietly(String keyOrUrl) {
		if (keyOrUrl == null || isExternal(keyOrUrl)) {
			return;
		}
		try {
			s3.deleteObject(b -> b.bucket(bucket).key(keyOrUrl));
		}
		catch (RuntimeException ex) {
			log.warn("Không xóa được ảnh {}: {}", keyOrUrl, ex.getMessage());
		}
	}

	/** Xóa các object cũ hơn minAge trong prefix mà isReferenced trả về false. */
	public int deleteOrphans(String prefix, Duration minAge, Predicate<String> isReferenced) {
		Instant cutoff = Instant.now().minus(minAge);
		int deleted = 0;
		for (S3Object object : s3.listObjectsV2Paginator(b -> b.bucket(bucket).prefix(prefix)).contents()) {
			if (object.lastModified().isBefore(cutoff) && !isReferenced.test(object.key())) {
				deleteQuietly(object.key());
				deleted++;
			}
		}
		return deleted;
	}

	private boolean isExternal(String value) {
		return value.startsWith("http://") || value.startsWith("https://");
	}

	/** Chỉ đọc header ảnh, không giải nén toàn bộ. */
	private Dimension readSize(byte[] bytes) {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				return null;
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(input);
				return new Dimension(reader.getWidth(0), reader.getHeight(0));
			}
			finally {
				reader.dispose();
			}
		}
		catch (IOException ex) {
			throw ApiException.badRequest("INVALID_IMAGE", "File ảnh bị hỏng");
		}
	}

	private enum ImageType {

		JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), WEBP("webp", "image/webp");

		final String extension;

		final String contentType;

		ImageType(String extension, String contentType) {
			this.extension = extension;
			this.contentType = contentType;
		}

		/** Xác định định dạng bằng magic bytes, không tin đuôi file hay Content-Type. */
		static ImageType detect(byte[] b) {
			if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
				return JPEG;
			}
			if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
				return PNG;
			}
			if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E'
					&& b[10] == 'B' && b[11] == 'P') {
				return WEBP;
			}
			return null;
		}

	}

}
