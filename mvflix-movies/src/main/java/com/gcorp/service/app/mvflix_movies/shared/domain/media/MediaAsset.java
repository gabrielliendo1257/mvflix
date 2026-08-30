package com.gcorp.service.app.mvflix_movies.shared.domain.media;

/** Common capability of domain objects that can provide a playable file. */
public interface MediaAsset {

    MediaAssetReference playbackReference();

    boolean isPlayable();
}
