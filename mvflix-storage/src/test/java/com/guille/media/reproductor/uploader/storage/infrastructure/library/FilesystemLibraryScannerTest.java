package com.guille.media.reproductor.uploader.storage.infrastructure.library;

import static org.assertj.core.api.Assertions.assertThat;

import com.guille.media.reproductor.uploader.storage.domain.vos.DiscoveredFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class FilesystemLibraryScannerTest {

    private final FilesystemLibraryScanner scanner = new FilesystemLibraryScanner();

    @TempDir Path tempDir;

    @Test
    void discoversSupportedFilesWithRelativePaths() throws IOException {
        Files.createDirectories(this.tempDir.resolve("Interstellar"));
        Files.write(this.tempDir.resolve("Interstellar/Interstellar.mkv"), new byte[] {1, 2, 3});
        Files.write(this.tempDir.resolve("Dune.mp4"), new byte[] {4, 5, 6, 7});

        var discovered =
                this.scanner.scan(this.tempDir.toString()).collectList().block();

        assertThat(discovered)
                .containsExactlyInAnyOrder(
                        new DiscoveredFile("Interstellar/Interstellar.mkv", 3, "video/x-matroska"),
                        new DiscoveredFile("Dune.mp4", 4, "video/mp4"));
    }

    @Test
    void skipsUnsupportedAndHiddenFiles() throws IOException {
        Files.write(this.tempDir.resolve("readme.txt"), new byte[] {1});
        Files.write(this.tempDir.resolve(".hidden.mkv"), new byte[] {2});
        Files.write(this.tempDir.resolve("matrix.webm"), new byte[] {3});

        StepVerifier.create(this.scanner.scan(this.tempDir.toString()))
                .expectNextMatches(file -> file.relativePath().equals("matrix.webm"))
                .verifyComplete();
    }

    @Test
    void ignoresNonExistentRoot() {
        StepVerifier.create(this.scanner.scan("/no/such/dir/here"))
                .verifyComplete();
    }

    @Test
    void skipsSymlinkEscapingTheRoot() throws IOException {
        Path outside = this.tempDir.getParent().resolve("outside.mkv");
        Files.write(outside, new byte[] {9});
        try {
            Files.createSymbolicLink(this.tempDir.resolve("escape.mkv"), outside);

            StepVerifier.create(this.scanner.scan(this.tempDir.toString()))
                    .verifyComplete();
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void returnsEmptyForDirectoryWithNoSupportedFiles() throws IOException {
        Files.createDirectories(this.tempDir.resolve("vacio"));

        StepVerifier.create(this.scanner.scan(this.tempDir.toString()))
                .verifyComplete();
    }
}