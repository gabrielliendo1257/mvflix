package com.guille.media.reproductor.users;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("sandbox")
@Testcontainers
class MvflixUsersApplicationTests {

	@Container
	static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
					.withDatabaseName("test-db")
					.withUsername("test")
					.withPassword("test");

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.r2dbc.url", () -> POSTGRES.getJdbcUrl().replace("jdbc:", "r2dbc:"));
		registry.add("spring.r2dbc.username", POSTGRES::getUsername);
		registry.add("spring.r2dbc.password", POSTGRES::getPassword);
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Test
	void contextLoads() {
	}

}
