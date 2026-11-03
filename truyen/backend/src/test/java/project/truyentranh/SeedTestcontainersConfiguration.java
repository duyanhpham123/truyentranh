package project.truyentranh;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/** Giống cấu hình test thường, nhưng database được nạp thêm dữ liệu mẫu 02_seed.sql. */
@TestConfiguration(proxyBeanMethods = false)
class SeedTestcontainersConfiguration extends TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	@Override
	MySQLContainer mysqlContainer() {
		return mysql("01_schema.sql", "02_seed.sql");
	}

}
