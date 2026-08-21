package com.gcorp.service.app.mvflix_movies.domain.mediaasset;

/**
 * Estado de identificación de un activo, ortogonal a su presencia en disco
 * (ver {@link MediaAsset#isPresent()}): un archivo puede estar IDENTIFIED y
 * a la vez haber desaparecido del filesystem.
 */
public enum MediaAssetStatus {
    /** Descubierto por el scan pero aun no vinculado a una pelicula. */
    UNIDENTIFIED,
    /** Vinculado a una pelicula del catalogo (movie_id no nulo). */
    IDENTIFIED;
}