package com.guille.media.bff.experience.playback.application;

/**
 * Contenido reproducible resuelto desde el catalogo (movies). Una media puede
 * terminar con varios assets; el catalogo decide cual aplica y en que estado
 * esta: aqui solo llega lo que movies ya considera vinculado a la media.
 *
 * <p>Dualidad explicita de storage: {@link #objectId} no nulo = MANAGED
 * (objeto en MinIO, acceso via presigned); si es nulo, el asset vive en una
 * biblioteca LOCAL ({@code libraryId + relativePath}) y se entrega via el
 * proxy autorizado del BFF mientras el navegador no alcance a storage.
 */
public record PlayableAsset(
    long assetId,
    long mediaId,
    String mimeType,
    long sizeBytes,
    Long objectId,
    Long libraryId,
    String relativePath) {

  public boolean isManaged() {
    return this.objectId != null;
  }
}
