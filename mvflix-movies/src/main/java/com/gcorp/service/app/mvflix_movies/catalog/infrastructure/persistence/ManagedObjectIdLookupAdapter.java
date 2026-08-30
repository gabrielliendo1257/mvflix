package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.application.ManagedObjectIdLookup;
import com.gcorp.service.app.mvflix_movies.catalog.domain.media.MediaRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ManagedObjectIdLookupAdapter implements ManagedObjectIdLookup {
    private final MediaRepository mediaRepository;

    @Override
    public Mono<Long> findObjectId(CatalogItemId movieId) {
        return this.mediaRepository.findByMovieId(movieId).map(media -> media.getObjectId());
    }
}
