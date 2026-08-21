package com.gcorp.service.app.mvflix_movies.application.enrichment;

import com.gcorp.service.app.mvflix_movies.app.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieDetail;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieSearch;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichMovieUseCase {

    private final MovieRepository movieRepository;
    private final MetadataSource metadataSource;
    private final UserProvider userProvider;

    /** Variante HTTP: resuelve la pelicula del usuario autenticado y la enriquece. */
    public Mono<Movie> enrichCurrentUser(MovieId id) {
        return this.enrichCurrentUser(id, null);
    }

    /**
     * Como {@link #enrichCurrentUser(MovieId)} pero si {@code explicitTmdbId} viene,
     * el usuario ya eligio el candidato en la UI y se salta el match automatico.
     */
    public Mono<Movie> enrichCurrentUser(MovieId id, Long explicitTmdbId) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .filter(movie -> movie.isOwnedBy(user.subject()))
                        .switchIfEmpty(Mono.error(
                                new MovieNotFoundException("Movie not found: " + id.value())))
                        .flatMap(movie -> this.enrich(movie, explicitTmdbId)));
    }

    /**
     * Desvincula la película del proveedor externo: limpia tmdbId/poster/popularity
     * y la devuelve a RAW para que el usuario rellene sus propios datos (media que
     * no representa una película). Solo el dueño.
     */
    public Mono<Movie> unlinkCurrentUser(MovieId id) {
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> this.movieRepository
                        .findById(id)
                        .filter(movie -> movie.isOwnedBy(user.subject()))
                        .switchIfEmpty(Mono.error(
                                new MovieNotFoundException("Movie not found: " + id.value())))
                        .flatMap(movie -> this.movieRepository.updateEnrichment(
                                id, movie.getMetadata().withoutProvider(),
                                EnrichmentStatus.RAW))
                        .doOnNext(unlinked -> log.info(
                                "Pelicula {} desvinculada del proveedor: queda RAW",
                                id.value())));
    }

    /** Busca candidatos en la fuente externa para que el usuario elija. */
    public Mono<List<ExternalMovieSearch>> search(String query, Integer year) {
        return this.metadataSource.searchCandidates(query, year).collectList();
    }

    /**
     * Preview de un candidato: la metadata que la fuente externa tiene para un
     * tmdb_id, sin persistir nada. Sirve para que el usuario confirme antes del enrich.
     */
    public Mono<MovieMetadata> preview(long tmdbId) {
        return this.metadataSource
                .findById(tmdbId)
                .map(EnrichMovieUseCase::fromDetail);
    }

    /** Metadata fresca solo desde el detalle externo (sin conservar la actual). */
    private static MovieMetadata fromDetail(ExternalMovieDetail detail) {
        return new MovieMetadata(
                detail.title(),
                detail.originalTitle(),
                detail.year(),
                detail.genres(),
                detail.popularity() > 0 ? detail.popularity() : null,
                detail.runtimeMinutes() > 0
                        ? formatDuration(detail.runtimeMinutes())
                        : null,
                detail.director(),
                detail.cast(),
                detail.overview(),
                detail.posterPath(),
                detail.releaseDate(),
                detail.country(),
                detail.language(),
                null,
                detail.tmdbId());
    }

    /**
     * Núcleo del enriquecimiento (sin seguridad, reutilizable por el scheduler):
     * idempotente (si ya ENRICHED no toca nada), matchea por tmdbId explícito,
     * por tmdbId persistido o por titulo+año; sin match deja la pelicula en RAW.
     */
    public Mono<Movie> enrich(Movie movie) {
        return this.enrich(movie, null);
    }

    public Mono<Movie> enrich(Movie movie, Long explicitTmdbId) {
        // El enriquecimiento solo aplica a items MOVIE; un OTHER (media que no
        // representa una película) nunca se enriquece ni se matchea con TMDB.
        if (!movie.isMovie()) {
            return Mono.just(movie);
        }
        // El auto-enriquecimiento (scheduler, sin tmdb_id) no re-enricha una película
        // ya ENRICHED; en cambio un tmdb_id explícito (el usuario re-matcher en la UI)
        // sí reemplaza el match anterior.
        if (movie.isEnriched() && explicitTmdbId == null) {
            log.info("Pelicula {} ya ENRICHED: no-op", movie.getId().value());
            return Mono.just(movie);
        }

        Long tmdbId = explicitTmdbId != null
                ? explicitTmdbId
                : movie.getMetadata().tmdbId();
        Mono<ExternalMovieDetail> detail = Mono.defer(() -> tmdbId != null
                ? this.metadataSource.findById(tmdbId)
                : this.metadataSource
                        .search(movie.getTitle(), movie.getMetadata().year())
                        .flatMap(search -> this.metadataSource.findById(search.tmdbId())));

        return detail
                .flatMap(d -> {
                    MovieMetadata metadata = isReMatch(movie, explicitTmdbId)
                            ? fromDetail(d)
                            : merge(movie.getMetadata(), d);
                    return this.movieRepository.updateEnrichment(
                            movie.getId(), metadata,
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

    /**
     * Re-match: el usuario eligió un tmdb_id distinto al persistido. En ese caso
     * la metadata se reemplaza por completo desde el nuevo detalle (sin conservar
     * campos viejos); si es el mismo o es el primer enrich, se fusiona.
     */
    private static boolean isReMatch(Movie movie, Long explicitTmdbId) {
        return explicitTmdbId != null
                && movie.getMetadata().tmdbId() != null
                && !explicitTmdbId.equals(movie.getMetadata().tmdbId());
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

    private static String formatDuration(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        return hours > 0 ? hours + "h " + rest + "m" : rest + "m";
    }
}