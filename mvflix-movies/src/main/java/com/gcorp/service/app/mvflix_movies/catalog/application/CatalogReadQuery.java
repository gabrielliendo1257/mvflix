package com.gcorp.service.app.mvflix_movies.catalog.application;

/**
 * Consulta ya normalizada para la proyección owned. La normalización
 * (tamaño de página, whitelist de orden) vive en el servicio de consulta;
 * el repositorio recibe valores seguros sin decidir política.
 */
public record CatalogReadQuery(
    String ownerUsername,
    int page,
    int size,
    String search,
    String status,
    SortField sort,
    boolean ascending,
    boolean isAdmin) {

  public enum SortField {
    TITLE("title"), YEAR("year"), UPDATED_AT("updated_at");

    private final String column;

    SortField(String column) {
      this.column = column;
    }

    public String column() {
      return this.column;
    }

    public static SortField from(String raw) {
      if (raw == null) {
        return UPDATED_AT;
      }
      return switch (raw.toLowerCase()) {
        case "title" -> TITLE;
        case "year" -> YEAR;
        default -> UPDATED_AT;
      };
    }
  }

  public int offset() {
    return this.page * this.size;
  }
}
