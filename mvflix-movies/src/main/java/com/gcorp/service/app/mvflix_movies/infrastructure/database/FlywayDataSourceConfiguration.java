package com.gcorp.service.app.mvflix_movies.infrastructure.database;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DataSource JDBC únicamente para que Flyway ejecute las migraciones.
 *
 * <p>La aplicación es R2DBC: Spring Boot no crea un DataSource JDBC cuando
 * existe un {@code ConnectionFactory} reactivo, por lo que sin este bean
 * Flyway no tendría dónde correr. Solo lo usa Flyway; el negocio opera
 * siempre sobre {@link org.springframework.r2dbc.core.DatabaseClient}.
 */
@Configuration
public class FlywayDataSourceConfiguration {

  @Bean
  DataSource flywayDataSource(
      @Value("${spring.datasource.url}") String url,
      @Value("${spring.datasource.username}") String username,
      @Value("${spring.datasource.password}") String password) {
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setMaximumPoolSize(2);
    return dataSource;
  }
}
