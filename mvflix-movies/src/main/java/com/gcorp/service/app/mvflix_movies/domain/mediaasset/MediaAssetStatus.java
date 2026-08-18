package com.gcorp.service.app.mvflix_movies.domain.mediaasset;

public enum MediaAssetStatus {
    /** Descubierto por el scan pero aun no vinculado a una pelicula. */
    UNIDENTIFIED,
    /** Vinculado a una pelicula del catalogo (movie_id no nulo). */
    IDENTIFIED,
    /** El archivo dejo de existir en el filesystem del operador. */
    MISSING;
}
