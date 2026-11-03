package project.truyentranh;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/** MySQL cùng phiên bản với máy dev, tạo schema bằng chính file trong thư mục database. */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	private static final String MINIO_USER = "testuser";

	private static final String MINIO_PASSWORD = "testpassword";

	static MySQLContainer mysql(String... scripts) {
		MySQLContainer container = new MySQLContainer(DockerImageName.parse("mysql:8.0.46")).withDatabaseName("comic_web");
		for (String script : scripts) {
			container.withCopyFileToContainer(MountableFile.forHostPath("../../database/" + script),
					"/docker-entrypoint-initdb.d/" + script);
		}
		return container;
	}

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return mysql("01_schema.sql");
	}

	@Bean
	@ServiceConnection
	RabbitMQContainer rabbitContainer() {
		return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.1-management"));
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
	}

	@Bean
	GenericContainer<?> minioContainer() {
		return new GenericContainer<>(DockerImageName.parse("cgr.dev/chainguard/minio:latest"))
			.withEnv("MINIO_ROOT_USER", MINIO_USER)
			.withEnv("MINIO_ROOT_PASSWORD", MINIO_PASSWORD)
			.withCommand("server", "/tmp/data")
			.withExposedPorts(9000)
			.waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));
	}

	@Bean
	DynamicPropertyRegistrar storageProperties(GenericContainer<?> minioContainer) {
		return registry -> {
			String url = "http://" + minioContainer.getHost() + ":" + minioContainer.getMappedPort(9000);
			registry.add("app.storage.endpoint", () -> url);
			registry.add("app.storage.access-key", () -> MINIO_USER);
			registry.add("app.storage.secret-key", () -> MINIO_PASSWORD);
			registry.add("app.storage.public-base-url", () -> url + "/truyentranh");
		};
	}

}
