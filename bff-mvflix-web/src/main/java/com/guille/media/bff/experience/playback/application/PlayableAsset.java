package com.guille.media.bff.experience.playback.application;

/**
 * Contenido reproducible resuelto desde el catálogo. Un asset tiene EXACTAMENTE
 * UN locator; esta clase es el único lugar del BFF que conoce esa convención
 * (transitional mientras movies exponga storageType explícito):
 *
 * <pre>
 * MANAGED: objectId != null  && libraryId == null && relativePath == null
 * LOCAL:   objectId == null  && libraryId != null && relativePath != null
 * </pre>
 *
 * {@code assetId} es el id del MediaAsset de catálogo; para MANAGED puede ser
 * {@code null} porque el objeto subido aún no genera MediaAsset.
 */
public record PlayableAsset(
    Long assetId,
    long mediaId,
    String mimeType,
    long sizeBytes,
    Long objectId,
    Long libraryId,
    String relativePath) {

  public PlayableAsset {
    boolean managed = objectId != null;
    boolean local = libraryId != null || relativePath != null;
    if (managed && local) {
      throw new IllegalArgumentException(
          "Locator ambiguo: objectId y biblioteca presentes para la media " + mediaId);
    }
    if (local && (libraryId == null || relativePath == null || relativePath.isBlank())) {
      throw new IllegalArgumentException(
          "Locator LOCAL incompleto para la media " + mediaId);
    }
  }

  public boolean isManaged() {
    return this.objectId != null;
  }
}
