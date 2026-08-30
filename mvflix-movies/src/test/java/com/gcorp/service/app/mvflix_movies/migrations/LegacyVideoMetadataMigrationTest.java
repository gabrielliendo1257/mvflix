package com.gcorp.service.app.mvflix_movies.migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gcorp.service.app.mvflix_movies.support.PostgresIntegrationTest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

class LegacyVideoMetadataMigrationTest extends PostgresIntegrationTest {

  @Test
  void normalizesLegacyMovieMetadataWhenVideoKindIsMigrated() throws SQLException {
    String schema = "migration_test_" + UUID.randomUUID().toString().replace('-', '_');
    Flyway flyway = configuration(schema).target("19").load();

    try {
      flyway.migrate();
      insertLegacyVideo(schema);

      configuration(schema).load().migrate();

      try (var connection = POSTGRES.createConnection("")) {
        try (var statement = connection.createStatement();
            ResultSet result = statement.executeQuery(
                "SELECT kind, metadata->>'title' AS title, "
                    + "metadata->>'description' AS description, "
                    + "metadata->>'recordedAt' AS recorded_at, "
                    + "metadata ? 'overview' AS has_overview, "
                    + "metadata ? 'genres' AS has_genres "
                    + "FROM " + schema + ".catalog_items WHERE id = 1")) {
          assertThat(result.next()).isTrue();
          assertThat(result.getString("kind")).isEqualTo("VIDEO");
          assertThat(result.getString("title")).isEqualTo("Legacy clip");
          assertThat(result.getString("description")).isEqualTo("Recorded before VIDEO existed");
          assertThat(result.getString("recorded_at")).isNull();
          assertThat(result.getBoolean("has_overview")).isFalse();
          assertThat(result.getBoolean("has_genres")).isFalse();
        }

        try (var statement = connection.createStatement();
            ResultSet result = statement.executeQuery(
                "SELECT version FROM " + schema + ".flyway_schema_history "
                    + "WHERE version = '20'")) {
           assertThat(result.next()).isTrue();
           assertThat(result.getString("version")).isEqualTo("20");
         }
      }
    } finally {
      flyway.clean();
    }
  }

  @Test
  void refusesLegacyRenditionDuplicatesWithoutChangingRowsOrApplyingV21() throws SQLException {
    String schema = "migration_test_" + UUID.randomUUID().toString().replace('-', '_');
    Flyway flyway = configuration(schema).target("19").load();

    try {
      flyway.migrate();
      insertDuplicateRenditions(schema);

      assertThatThrownBy(() -> configuration(schema).load().migrate())
          .hasMessageContaining(
              "Cannot create rendition uniqueness: duplicate source/profile rows exist");

      try (var connection = POSTGRES.createConnection("");
          var statement = connection.createStatement()) {
        try (ResultSet result = statement.executeQuery(
            "SELECT status, storage_object_id FROM " + schema
                + ".media_asset_renditions WHERE media_asset_id = 1 "
                + "AND profile = '1080p' ORDER BY id")) {
          assertThat(result.next()).isTrue();
          assertThat(result.getString("status")).isEqualTo("REQUESTED");
          assertThat(result.getObject("storage_object_id", Long.class)).isNull();
          assertThat(result.next()).isTrue();
          assertThat(result.getString("status")).isEqualTo("READY");
          assertThat(result.getLong("storage_object_id")).isEqualTo(101L);
          assertThat(result.next()).isFalse();
        }

        try (ResultSet result = statement.executeQuery(
            "SELECT COUNT(*) AS count FROM " + schema
                + ".flyway_schema_history WHERE version = '21'")) {
          assertThat(result.next()).isTrue();
          assertThat(result.getLong("count")).isZero();
        }
      }
    } finally {
      flyway.clean();
    }
  }

  private static FluentConfiguration configuration(String schema) {
    return Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .schemas(schema)
        .defaultSchema(schema)
        .locations("classpath:db/migration")
        .baselineOnMigrate(false)
        .cleanDisabled(false);
  }

  private void insertLegacyVideo(String schema) throws SQLException {
    try (var connection = POSTGRES.createConnection("")) {
      try (var statement = connection.prepareStatement(
          "INSERT INTO " + schema + ".catalog_items "
              + "(owner_username, title, status, enrichment_status, metadata, visibility, kind) "
              + "VALUES (?, ?, 'DRAFT', 'RAW', ?::jsonb, 'PRIVATE', 'VIDEO')")) {
        statement.setString(1, "migration-test");
        statement.setString(2, "Legacy clip");
        statement.setString(
            3,
            "{\"title\":\"Legacy clip\",\"originalTitle\":\"Old title\","
                + "\"genres\":[\"documentary\"],\"overview\":\"Recorded before VIDEO existed\"}");
        statement.executeUpdate();
      }
    }
  }

  private void insertDuplicateRenditions(String schema) throws SQLException {
    try (var connection = POSTGRES.createConnection("")) {
      try (var statement = connection.createStatement()) {
        statement.executeUpdate(
            "INSERT INTO " + schema + ".media_assets "
                + "(library_id, relative_path, size, mime_type, status) "
                + "VALUES (1, 'legacy-video.mp4', 1, 'video/mp4', 'UNIDENTIFIED')");
        statement.executeUpdate(
            "INSERT INTO " + schema + ".media_asset_renditions "
                + "(media_asset_id, storage_object_id, profile, status) VALUES "
                + "(1, NULL, '1080p', 'REQUESTED'), (1, 101, '1080p', 'READY')");
      }
    }
  }
}
