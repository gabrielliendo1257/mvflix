package com.gcorp.service.app.mvflix_movies.application.enrichment;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieDetail;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.MetadataSource;
import com.gcorp.service.app.mvflix_movies.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichMovieUseCase {

    private final MovieRepository movieRepository;
    private final MetadataSource metadataSource;
    private final UserProvider userProvider;

    /** Variante HTTP: resuelve la pelicula del usuario autenticado y la enriquece. */
    public Mono<Movie> enrichCurrentUser(MovieId id) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .filter(movie -> movie.getOwnerUsername().equals(user.subject()))
                        .switchIfEmpty(Mono.error(
                                new MovieNotFoundException("Movie not found: " + id.value())))
                        .flatMap(this::enrich));
    }

    /**
     * Núcleo del enriquecimiento (sin seguridad, reutilizable por el scheduler):
     * idempotente (si ya ENRICHED no toca nada), matchea por tmdbId persistido
     * o por titulo+año, y sin match deja la pelicula en RAW sin inventar datos.
     */
    public Mono<Movie> enrich(Movie movie) {
        if (movie.isEnriched()) {
            log.info("Pelicula {} ya ENRICHED: no-op", movie.getId().value());
            return Mono.just(movie);
        }

        Long tmdbId = movie.getMetadata().tmdbId();
        Mono<ExternalMovieDetail> detail = tmdbId != null
                ? this.metadataSource.findById(tmdbId)
                : this.metadataSource
                        .search(movie.getTitle(), movie.getMetadata().year())
                        .flatMap(search -> this.metadataSource.findById(search.tmdbId()));

        return detail
                .flatMap(d -> {
                    MovieMetadata merged = this.merge(movie.getMetadata(), d);
                    return this.movieRepository.updateEnrichment(
                            movie.getId(), movie.getOwnerUsername(), merged,
                            EnrichmentStatus.ENRICHED);
                })
                .doOnNext(enriched -> log.info(
                        "Pelicula {} enriquecida: tmdb_id={}",
                        movie.getId().value(), enriched.getMetadata().tmdbId()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Pelicula {} sin match en la fuente externa: queda RAW",
                            movie.getId().value());
                    return Mono.just(movie);
                }));
    }

    /** Funde la metadata externa con la actual: la fuente externa gana, los huecos se respetan. */
    private MovieMetadata merge(MovieMetadata current, ExternalMovieDetail detail) {
        return new MovieMetadata(
                detail.title() != null ? detail.title() : current.title(),
                detail.originalTitle() != null ? detail.originalTitle() : current.originalTitle(),
                detail.year() != null ? detail.year() : current.year(),
                !detail.genres().isEmpty() ? detail.genres() : current.genres(),
                detail.popularity() > 0 ? detail.popularity() : current.popularity(),
                detail.runtimeMinutes() > 0
                        ? formatDuration(detail.runtimeMinutes())
                        : current.duration(),
                detail.director() != null ? detail.director() : current.director(),
                !detail.cast().isEmpty() ? detail.cast() : current.cast(),
                detail.overview() != null ? detail.overview() : current.overview(),
                detail.posterPath() != null ? detail.posterPath() : current.posterPath(),
                detail.releaseDate() != null ? detail.releaseDate() : current.releaseDate(),
                detail.country() != null ? detail.country() : current.country(),
                detail.language() != null ? detail.language() : current.language(),
                current.awards(),
                detail.tmdbId());
    }

    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        return hours > 0 ? hours + "h " + rest + "m" : rest + "m";
    }
}