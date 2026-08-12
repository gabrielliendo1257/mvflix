package com.guille.media.reproductor.users;

import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Base de los tests de integración R2DBC: levanta un Postgres real en
 * Testcontainers, ejecuta las migraciones SQL de {@code db/migration} al
 * arrancar (mediante {@code /docker-entrypoint-initdb.d}) y expone las
 * properties {@code spring.r2dbc.*} apuntando al container.
 */
@Testcontainers
@DataR2dbcTest
public abstract class AbstractR2dbcIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("test-db")
                    .withUsername("test")
                    .withPassword("test")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(
                                    "db/migration"),
                            "/docker-entrypoint-initdb.d");

    @DynamicPropertySource
    static void r2dbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> POSTGRES.getJdbcUrl().replace("jdbc:", "r2dbc:"));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }
}