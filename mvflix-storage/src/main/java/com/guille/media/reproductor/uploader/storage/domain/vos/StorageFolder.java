package com.guille.media.reproductor.uploader.storage.domain.vos;

/**
 * Estructura de carpetas dentro del espacio de cada usuario en el bucket dedicado.
 *
 * <p>Cada usuario tiene su raíz {@code <username>/} con estas subcarpetas fijas.
 */
public enum StorageFolder {
  IMAGES("images/"),
  VIDEOS("videos/"),
  COMPRESSED("compressed/"),
  EXECUTABLES("executables/"),
  PRIVATE("private/");

  private final String path;

  StorageFolder(String path) {
    this.path = path;
  }

  public String path() {
    return path;
  }

  /**
   * Resuelve la carpeta destino según el tipo MIME (default: privado).
   */
  public static StorageFolder from(MimeType mimeType) {
    String value = mimeType.value();

    if (value.startsWith("video/")) {
      return VIDEOS;
    }
    if (value.startsWith("image/")) {
      return IMAGES;
    }
    if (value.contains("zip")
        || value.contains("compressed")
        || value.contains("tar")
        || value.contains("gzip")
        || value.contains("rar")
        || value.contains("7z")) {
      return COMPRESSED;
    }
    if (value.contains("executable")
        || value.contains("octet-stream")
        || value.equals("application/x-sh")
        || value.equals("application/x-msdownload")) {
      return EXECUTABLES;
    }
    return PRIVATE;
  }
}