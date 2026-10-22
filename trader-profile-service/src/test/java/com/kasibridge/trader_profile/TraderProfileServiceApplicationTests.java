package com.kasibridge.trader_profile;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
		properties = {
				"spring.jpa.hibernate.ddl-auto=create-drop"
		}
)
@Testcontainers
@SuppressWarnings("resource")
class TraderProfileServiceApplicationTests {

	@Container
	static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>(
					"postgres:16-alpine"
			)
					.withDatabaseName(
							"trader_profile_context_test"
					)
					.withUsername("test")
					.withPassword("test");

	@DynamicPropertySource
	static void configureDatasource(
			DynamicPropertyRegistry registry
	) {
		registry.add(
				"spring.datasource.url",
				POSTGRES::getJdbcUrl
		);

		registry.add(
				"spring.datasource.username",
				POSTGRES::getUsername
		);

		registry.add(
				"spring.datasource.password",
				POSTGRES::getPassword
		);

		registry.add(
				"spring.datasource.driver-class-name",
				POSTGRES::getDriverClassName
		);
	}

	@Test
	void contextLoads() {
	}
}