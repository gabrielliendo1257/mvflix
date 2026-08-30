package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

import com.gcorp.service.app.mvflix_movies.shared.domain.media.MediaAssetReference;

/** Common capability of domain objects that can provide a playable file. */
public interface MediaAsset {

    String getFilename();

    Long getDuration();

    String getContainer();

    String getVideoCodec();

    String getResolution();

    String getStorageReference();

    MediaAssetReference playbackReference();

    boolean isPlayable();
}
