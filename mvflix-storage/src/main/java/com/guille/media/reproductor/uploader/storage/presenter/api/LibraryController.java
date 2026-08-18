package com.guille.media.reproductor.uploader.storage.presenter.api;

import com.guille.media.reproductor.uploader.storage.app.service.LibraryService;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.DiscoveredFileResponse;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.LibraryResponse;
import com.guille.media.reproductor.uploader.storage.presenter.mapper.LibraryMapper;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/api/v1/movie/storage/libraries", produces = MediaType.APPLICATION_JSON_VALUE)
public class LibraryController {

    private final LibraryService libraryService;
    private final LibraryMapper libraryMapper;

    public LibraryController(LibraryService libraryService, LibraryMapper libraryMapper) {
        this.libraryService = libraryService;
        this.libraryMapper = libraryMapper;
    }

    @GetMapping
    public Flux<LibraryResponse> libraries() {
        return this.libraryService.listLibraries().map(this.libraryMapper::toLibraryResponse);
    }

    @GetMapping(value = "/{libraryId}/files")
    public Flux<DiscoveredFileResponse> libraryFiles(@PathVariable Long libraryId) {
        return this.libraryService
                .scanLibrary(libraryId)
                .map(this.libraryMapper::toDiscoveredFileResponse);
    }
}