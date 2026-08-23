package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MediaKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieVisibility;
import com.gcorp.service.app.mvflix_movies.library.application.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.library.domain.CatalogItemId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LibraryCatalogItemCreatorTest {

    @Mock private MovieRepository movieRepository;

    @InjectMocks private LibraryCatalogItemCreator creator;

    @Test
    void catalogOwnsHowALibraryItemIsCreated() {
        when(this.movieRepository.save(any(Movie.class)))
                .thenAnswer(invocation -> {
                    Movie movie = invocation.getArgument(0);
                    return Mono.just(new Movie(
                            MovieId.of(50L),
                            movie.getOwnerUsername(),
                            movie.getTitle(),
                            movie.getStatus(),
                            movie.getEnrichmentStatus(),
                            movie.getObjectId(),
                            movie.getMetadata(),
                            movie.getVisibility(),
                            movie.getSharedWith(),
                            movie.getKind()));
                });

        StepVerifier.create(this.creator.createFromLibrary(
                        "Javier", "Dune", CatalogItemKind.MOVIE))
                .expectNext(CatalogItemId.of(50L))
                .verifyComplete();

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(this.movieRepository).save(captor.capture());
        Movie newMovie = captor.getValue();
        assertThat(newMovie.getOwnerUsername()).isEqualTo("Javier");
        assertThat(newMovie.getTitle()).isEqualTo("Dune");
        assertThat(newMovie.getStatus()).isEqualTo(MovieStatus.READY);
        assertThat(newMovie.getEnrichmentStatus()).isEqualTo(EnrichmentStatus.RAW);
        assertThat(newMovie.getVisibility()).isEqualTo(MovieVisibility.PRIVATE);
        assertThat(newMovie.getObjectId()).isNull();
        assertThat(newMovie.getKind()).isEqualTo(MediaKind.MOVIE);
    }

    @Test
    void translatesOtherLibraryKindToCatalogClassification() {
        when(this.movieRepository.save(any(Movie.class)))
                .thenAnswer(invocation -> {
                    Movie movie = invocation.getArgument(0);
                    return Mono.just(new Movie(
                            MovieId.of(51L),
                            movie.getOwnerUsername(),
                            movie.getTitle(),
                            movie.getStatus(),
                            movie.getEnrichmentStatus(),
                            movie.getObjectId(),
                            movie.getMetadata(),
                            movie.getVisibility(),
                            movie.getSharedWith(),
                            movie.getKind()));
                });

        StepVerifier.create(this.creator.createFromLibrary(
                        "Javier", "Live concert", CatalogItemKind.OTHER))
                .expectNext(CatalogItemId.of(51L))
                .verifyComplete();

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(this.movieRepository).save(captor.capture());
        assertThat(captor.getValue().getKind()).isEqualTo(MediaKind.OTHER);
    }
}
