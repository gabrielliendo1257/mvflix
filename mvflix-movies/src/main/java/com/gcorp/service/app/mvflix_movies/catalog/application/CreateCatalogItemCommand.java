package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;

public record CreateCatalogItemCommand(MovieMetadata metadata, CatalogItemKind kind) {}
