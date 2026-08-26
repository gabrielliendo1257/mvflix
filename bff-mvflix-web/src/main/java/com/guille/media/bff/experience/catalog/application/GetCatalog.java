package com.guille.media.bff.experience.catalog.application;

import com.guille.media.bff.app.dto.MovieDto;
import com.guille.media.bff.app.dto.MovieListItemDto;
import com.guille.media.bff.app.ports.MoviesWebClient;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * "Administrar mi contenido": grilla del dueño con sus películas, base para
 * las filas que llevan a Media Detail (editar/borrar). Por ser OWNED, nunca
 * aparecen películas públicas ajenas con acciones que habría que ocultar.
 *
 * <p>Distinto de la búsqueda global (experience/search), que responde
 * "¿qué puedo ENCONTRAR o reproducir?" sobre el catálogo visible.
 */
@Service
@RequiredArgsConstructor
public class GetCatalog {

  private final MoviesWebClient movies;

  public Flux<MovieListItemDto> execute(CatalogQuery query) {
    return this.movies
        .listOwnedMovies(query.limit())
        .map(movie -> new MovieListItemDto(
            movie.id(), movie.status(), movie.visibility(), movie.kind(), movie.title(),
            movie.year(), movie.posterPath()));
  }
}
