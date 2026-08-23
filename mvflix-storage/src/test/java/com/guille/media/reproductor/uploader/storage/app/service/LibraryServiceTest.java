package com.guille.media.reproductor.uploader.storage.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guille.media.reproductor.uploader.storage.app.security.AuthenticatedUser;
import com.guille.media.reproductor.uploader.storage.app.security.UserProvider;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.domain.models.MediaLibraryType;
import com.guille.media.reproductor.uploader.storage.domain.ports.LibraryScanner;
import com.guille.media.reproductor.uploader.storage.domain.ports.MediaLibraryRepository;
import com.guille.media.reproductor.uploader.storage.infrastructure.errors.EntityNotFound;
import com.guille.media.reproductor.uploader.storage.infrastructure.library.ScanCancellationRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class LibraryServiceTest {

  private final MediaLibraryRepository libraryRepository = mock(MediaLibraryRepository.class);
  private final LibraryScanner libraryScanner = mock(LibraryScanner.class);
  private final UserProvider userProvider = mock(UserProvider.class);
  private final ScanCancellationRegistry cancellationRegistry =
      mock(ScanCancellationRegistry.class);

  private final LibraryService service =
      new LibraryService(
          this.libraryRepository, this.libraryScanner, this.userProvider,
          this.cancellationRegistry);

  private static final AuthenticatedUser PEPE = new AuthenticatedUser("pepe", "pepe@mvflix.dev");

  @BeforeEach
  void setUp() {
    when(this.userProvider.getAuthenticatedUser()).thenReturn(Mono.just(PEPE));
  }

  @Test
  void scanLibraryAllowsOperatorLibraryForAnyAuthenticatedUser() {
    MediaLibrary operatorLibrary = MediaLibrary.create(MediaLibraryType.LOCAL, "/tmp/media");
    when(this.libraryRepository.findById(1L)).thenReturn(Mono.just(operatorLibrary));
    when(this.libraryScanner.scan("/tmp/media")).thenReturn(Flux.empty());

    StepVerifier.create(this.service.scanLibrary(1L)).verifyComplete();

    verify(this.libraryScanner).scan("/tmp/media");
  }

  @Test
  void scanLibraryDeniesPrivateLibraryOfAnotherUser() {
    MediaLibrary anasLibrary = MediaLibrary.createOwned(MediaLibraryType.LOCAL, "/tmp/ana", "ana");
    when(this.libraryRepository.findById(2L)).thenReturn(Mono.just(anasLibrary));

    StepVerifier.create(this.service.scanLibrary(2L))
        .expectError(EntityNotFound.class)
        .verify();

    verifyNoInteractions(this.libraryScanner);
  }

  @Test
  void cancelScanDeniesPrivateLibraryOfAnotherUser() {
    MediaLibrary anasLibrary = MediaLibrary.createOwned(MediaLibraryType.LOCAL, "/tmp/ana", "ana");
    when(this.libraryRepository.findById(2L)).thenReturn(Mono.just(anasLibrary));

    StepVerifier.create(this.service.cancelScan(2L))
        .expectError(EntityNotFound.class)
        .verify();

    verifyNoInteractions(this.cancellationRegistry);
  }

  @Test
  void cancelScanCancelsAccessibleLibraryByRootPath() {
    MediaLibrary operatorLibrary = MediaLibrary.create(MediaLibraryType.LOCAL, "/tmp/media");
    when(this.libraryRepository.findById(1L)).thenReturn(Mono.just(operatorLibrary));

    StepVerifier.create(this.service.cancelScan(1L)).verifyComplete();

    verify(this.cancellationRegistry).cancel("/tmp/media");
  }

  @Test
  void findAccessibleLibraryReturnsNotFoundWhenMissingOrDisabled() {
    when(this.libraryRepository.findById(9L)).thenReturn(Mono.empty());
    MediaLibrary disabled = new MediaLibrary(
        8L, MediaLibraryType.LOCAL, "/tmp/x", false, null, java.time.Instant.now());
    when(this.libraryRepository.findById(8L)).thenReturn(Mono.just(disabled));

    StepVerifier.create(this.service.findAccessibleLibrary(9L))
        .expectError(EntityNotFound.class)
        .verify();
    StepVerifier.create(this.service.findAccessibleLibrary(8L))
        .expectError(EntityNotFound.class)
        .verify();

    assertThat(true).isTrue();
  }
}
