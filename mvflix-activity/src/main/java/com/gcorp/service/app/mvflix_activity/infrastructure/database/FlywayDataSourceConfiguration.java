package com.gcorp.service.app.mvflix_activity.infrastructure.database;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayDataSourceConfiguration {
  @Bean DataSource flywayDataSource(@Value("${spring.datasource.url}") String url,
      @Value("${spring.datasource.username}") String username, @Value("${spring.datasource.password}") String password) {
    HikariDataSource ds = new HikariDataSource(); ds.setJdbcUrl(url); ds.setUsername(username); ds.setPassword(password); ds.setMaximumPoolSize(2); return ds;
  }
}
