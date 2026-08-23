package com.guille.media.reproductor.uploader.storage.app.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.app.configuration.LibraryRegistryProperties;
import com.guille.media.reproductor.uploader.storage.app.security.AuthenticatedUser;
import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryAlreadyExistsException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryPathInvalidException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.LibraryPathNotAllowedException;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibraryType;
import com.guille.media.reproductor.uploader.storage.domain.ports.LibraryRootResolver;
import com.guille.media.reproductor.uploader.storage.domain.ports.MediaLibraryRepository;
import com.guille.media.reproductor.uploader.storage.infrastructure.library.FilesystemLibraryRootResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RegisterLibraryUseCaseTest {

    @TempDir Path tempDir;

    @Mock private MediaLibraryRepository libraryRepository;
    @Mock private UserProvider userProvider;

    private LibraryRootResolver rootResolver;
    private LibraryRegistryProperties properties;
    private RegisterLibraryUseCase useCase;

    @BeforeEach
    void setUp() {
        this.properties = new LibraryRegistryProperties();
        this.properties.setAllowedRoots(List.of(this.tempDir.toString()));
        this.rootResolver = new FilesystemLibraryRootResolver(this.properties);
        this.useCase =
                new RegisterLibraryUseCase(this.libraryRepository, this.userProvider, this.rootResolver);
    }

    private void authenticateAsJavier() {
        when(this.userProvider.getAuthenticatedUser())
                .thenReturn(Mono.just(new AuthenticatedUser("Javier", "javier@test")));
    }

    @Test
    void registersExistingDirectoryInsideAllowedRoot() throws Exception {
        this.authenticateAsJavier();
        Path sub = Files.createDirectory(this.tempDir.resolve("familia"));
        String realSub = sub.toRealPath().toString();
        when(this.libraryRepository.findByRootPath(any(String.class)))
                .thenReturn(Mono.empty());
        when(this.libraryRepository.save(any(MediaLibrary.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.<MediaLibrary>getArgument(0)));

        StepVerifier.create(this.useCase.execute(sub.toString()))
                .expectNextMatches(library ->
                        library.isOwnedBy("Javier")
                                && library.getType() == MediaLibraryType.LOCAL
                                && library.getRootPath().equals(realSub))
                .verifyComplete();

        verify(this.libraryRepository).save(any(MediaLibrary.class));
    }

    @Test
    void rejectsNonExistingDirectory() {
        String missing = this.tempDir.resolve("no-existe").toString();
        StepVerifier.create(this.useCase.execute(missing))
                .expectError(LibraryPathInvalidException.class)
                .verify();
    }

    @Test
    void rejectsFileInsteadOfDirectory() throws Exception {
        Path file = Files.createFile(this.tempDir.resolve("nota.txt"));
        StepVerifier.create(this.useCase.execute(file.toString()))
                .expectError(LibraryPathInvalidException.class)
                .verify();
    }

    @Test
    void rejectsPathOutsideAllowedRoot() {
        StepVerifier.create(this.useCase.execute("/etc"))
                .expectError(LibraryPathNotAllowedException.class)
                .verify();
    }

    @Test
    void allowAnyRootAcceptsPathOutsideAllowedRoots() throws Exception {
        this.properties.setAllowAnyRoot(true);
        this.authenticateAsJavier();
        Path sub = Files.createDirectory(this.tempDir.resolve("usb"));
        String realSub = sub.toRealPath().toString();
        when(this.libraryRepository.findByRootPath(any(String.class)))
                .thenReturn(Mono.empty());
        when(this.libraryRepository.save(any(MediaLibrary.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.<MediaLibrary>getArgument(0)));

        StepVerifier.create(this.useCase.execute(sub.toString()))
                .expectNextMatches(library ->
                        library.isOwnedBy("Javier")
                                && library.getRootPath().equals(realSub))
                .verifyComplete();

        verify(this.libraryRepository).save(any(MediaLibrary.class));
    }

    @Test
    void rejectsPathAlreadyRegisteredByOtherUser() {
        this.authenticateAsJavier();
        when(this.libraryRepository.findByRootPath(any(String.class)))
                .thenReturn(Mono.just(MediaLibrary.createOwned(
                        MediaLibraryType.LOCAL, this.tempDir.toString(), "Maria")));

        StepVerifier.create(this.useCase.execute(this.tempDir.toString()))
                .expectError(LibraryAlreadyExistsException.class)
                .verify();
    }
}