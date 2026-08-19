package com.guille.media.reproductor.uploader.storage.presenter.api;

import com.guille.media.reproductor.uploader.storage.app.service.DeleteLibraryUseCase;
import com.guille.media.reproductor.uploader.storage.app.service.LibraryService;
import com.guille.media.reproductor.uploader.storage.app.service.RegisterLibraryUseCase;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.RegisterLibraryRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.DiscoveredFileResponse;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.LibraryResponse;
import com.guille.media.reproductor.uploader.storage.presenter.mapper.LibraryMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/v1/movie/storage/libraries", produces = MediaType.APPLICATION_JSON_VALUE)
public class LibraryController {

    private final LibraryService libraryService;
    private final RegisterLibraryUseCase registerLibraryUseCase;
    private final DeleteLibraryUseCase deleteLibraryUseCase;
    private final LibraryMapper libraryMapper;

    public LibraryController(
            LibraryService libraryService,
            RegisterLibraryUseCase registerLibraryUseCase,
            DeleteLibraryUseCase deleteLibraryUseCase,
            LibraryMapper libraryMapper) {
        this.libraryService = libraryService;
        this.registerLibraryUseCase = registerLibraryUseCase;
        this.deleteLibraryUseCase = deleteLibraryUseCase;
        this.libraryMapper = libraryMapper;
    }

    @GetMapping
    public Flux<LibraryResponse> libraries() {
        return this.libraryService.listLibraries().map(this.libraryMapper::toLibraryResponse);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LibraryResponse>> register(
            @RequestBody(required = false) RegisterLibraryRequest request) {
        String rootPath = request == null ? null : request.rootPath();
        return this.registerLibraryUseCase
                .execute(rootPath)
                .map(this.libraryMapper::toLibraryResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @DeleteMapping(value = "/{libraryId}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long libraryId) {
        return this.deleteLibraryUseCase
                .execute(libraryId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping(value = "/{libraryId}/files")
    public Flux<DiscoveredFileResponse> libraryFiles(@PathVariable Long libraryId) {
        return this.libraryService
                .scanLibrary(libraryId)
                .map(this.libraryMapper::toDiscoveredFileResponse);
    }
}