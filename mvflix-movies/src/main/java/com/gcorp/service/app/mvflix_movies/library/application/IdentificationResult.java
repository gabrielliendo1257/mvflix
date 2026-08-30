package com.gcorp.service.app.mvflix_movies.library.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAsset;

record IdentificationResult(MediaAsset asset, CatalogItemId catalogItemId) {}
