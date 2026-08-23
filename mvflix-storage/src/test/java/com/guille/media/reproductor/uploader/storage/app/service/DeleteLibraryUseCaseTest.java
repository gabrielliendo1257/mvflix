package com.guille.media.reproductor.uploader.storage.app.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.app.security.AuthenticatedUser;
import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryAccessDeniedException;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibraryType;
import com.guille.media.reproductor.uploader.storage.domain.ports.MediaLibraryRepository;
import com.guille.media.reproductor.uploader.storage.app.errors.EntityNotFound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class DeleteLibraryUseCaseTest {

    @Mock private MediaLibraryRepository libraryRepository;
    @Mock private UserProvider userProvider;

    private DeleteLibraryUseCase useCase;

    @BeforeEach
    void setUp() {
        this.useCase = new DeleteLibraryUseCase(this.libraryRepository, this.userProvider);
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "javier@test")));
    }

    @Test
    void ownerDeletesOwnLibrary() {
        MediaLibrary library = new MediaLibrary(
                5L, MediaLibraryType.LOCAL, "/tmp/x", true, "Javier", Instant.now());
        when(this.libraryRepository.findById(5L)).thenReturn(Mono.just(library));
        when(this.libraryRepository.deleteById(5L)).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(5L)).verifyComplete();

        verify(this.libraryRepository).deleteById(5L);
    }

    @Test
    void otherUserCannotDeleteLibrary() {
        MediaLibrary library = new MediaLibrary(
                5L, MediaLibraryType.LOCAL, "/tmp/x", true, "Maria", Instant.now());
        when(this.libraryRepository.findById(5L)).thenReturn(Mono.just(library));

        StepVerifier.create(this.useCase.execute(5L))
                .expectError(LibraryAccessDeniedException.class)
                .verify();

        verify(this.libraryRepository, never()).deleteById(5L);
    }

    @Test
    void operatorLibraryCannotBeDeletedByUser() {
        MediaLibrary library = new MediaLibrary(
                1L, MediaLibraryType.LOCAL, "/tmp/y", true, null, Instant.now());
        when(this.libraryRepository.findById(1L)).thenReturn(Mono.just(library));

        StepVerifier.create(this.useCase.execute(1L))
                .expectError(LibraryAccessDeniedException.class)
                .verify();
    }

    @Test
    void missingLibraryIsNotFound() {
        when(this.libraryRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(this.useCase.execute(99L))
                .expectError(EntityNotFound.class)
                .verify();
    }
}