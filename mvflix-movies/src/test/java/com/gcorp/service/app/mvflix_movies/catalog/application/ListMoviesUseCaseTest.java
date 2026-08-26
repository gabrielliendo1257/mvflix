package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Movie;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ListMoviesUseCaseTest {

    @Mock private MovieRepository movieRepository;
    @Mock private UserProvider userProvider;

    @InjectMocks private ListMoviesUseCase useCase;

    private void userIs(String subject) {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser(subject, subject + "@m.com")));
    }

    @Test
    void visibleScopeQueriesVisibleMovies() {
        this.userIs("Javier");
        when(this.movieRepository.findVisibleMovies("Javier", 20)).thenReturn(Flux.empty());

        this.useCase.execute("visible", 20).collectList().block();

        verify(this.movieRepository).findVisibleMovies("Javier", 20);
    }

    @Test
    void unknownScopeFallsBackToVisible() {
        this.userIs("Javier");
        when(this.movieRepository.findVisibleMovies("Javier", 20)).thenReturn(Flux.empty());

        this.useCase.execute("cualquier-cosa", 20).collectList().block();

        verify(this.movieRepository).findVisibleMovies("Javier", 20);
        org.mockito.Mockito.verify(this.movieRepository, org.mockito.Mockito.never())
                .findByOwner(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void ownedScopeQueriesOnlyOwnContent() {
        this.userIs("Javier");
        when(this.movieRepository.findByOwner("Javier", 20)).thenReturn(Flux.empty());

        this.useCase.execute("owned", 20).collectList().block();

        verify(this.movieRepository).findByOwner("Javier", 20);
        org.mockito.Mockito.verify(this.movieRepository, org.mockito.Mockito.never())
                .findVisibleMovies(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void limitIsCappedToMaximum() {
        this.userIs("Javier");
        when(this.movieRepository.findByOwner("Javier", ListMoviesUseCase.MAX_LIMIT))
                .thenReturn(Flux.empty());

        this.useCase.execute("owned", 5000).collectList().block();

        verify(this.movieRepository).findByOwner("Javier", ListMoviesUseCase.MAX_LIMIT);
    }
}
