package com.guille.media.reproductor.uploader.storage.library.infrastructure.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibrary;
import com.guille.media.reproductor.uploader.storage.library.domain.model.MediaLibraryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class LocalLibraryFileResolverTest {

    private final LocalLibraryFileResolver resolver = new LocalLibraryFileResolver();

    @TempDir Path tempDir;

    @Test
    void resolvesFileInsideRoot() throws IOException {
        Files.createDirectories(this.tempDir.resolve("Carpetas"));
        Files.write(this.tempDir.resolve("Carpetas/Dune.mkv"), new byte[] {1, 2, 3});
        MediaLibrary library = MediaLibrary.create(MediaLibraryType.LOCAL, this.tempDir.toString());

        StepVerifier.create(this.resolver.resolve(library, "Carpetas/Dune.mkv"))
                .expectNextMatches(handle ->
                        handle.relativePath().equals("Carpetas/Dune.mkv")
                                && handle.size() == 3
                                && handle.mimeType().equals("video/x-matroska"))
                .verifyComplete();
    }

    @Test
    void rejectsPathTraversalOutsideRoot() throws IOException {
        Path outside = this.tempDir.getParent().resolve("secret.mkv");
        Files.write(outside, new byte[] {9});
        try {
            MediaLibrary library =
                    MediaLibrary.create(MediaLibraryType.LOCAL, this.tempDir.toString());

            StepVerifier.create(this.resolver.resolve(library, "../secret.mkv"))
                    .verifyComplete();
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void rejectsSymlinkEscapingTheRoot() throws IOException {
        Path outside = this.tempDir.getParent().resolve("escape.mkv");
        Files.write(outside, new byte[] {9});
        try {
            Files.createSymbolicLink(this.tempDir.resolve("escape.mkv"), outside);
            MediaLibrary library =
                    MediaLibrary.create(MediaLibraryType.LOCAL, this.tempDir.toString());

            StepVerifier.create(this.resolver.resolve(library, "escape.mkv"))
                    .verifyComplete();
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void emptyWhenFileDoesNotExist() {
        MediaLibrary library = MediaLibrary.create(MediaLibraryType.LOCAL, this.tempDir.toString());

        StepVerifier.create(this.resolver.resolve(library, "no-such.mkv"))
                .verifyComplete();
    }

    @Test
    void emptyWhenRootDoesNotExist() {
        MediaLibrary library =
                MediaLibrary.create(MediaLibraryType.LOCAL, "/no/such/root");

        StepVerifier.create(this.resolver.resolve(library, "Dune.mkv"))
                .verifyComplete();
    }
}