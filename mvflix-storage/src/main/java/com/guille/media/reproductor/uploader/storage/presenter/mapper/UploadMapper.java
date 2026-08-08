package com.guille.media.reproductor.uploader.storage.presenter.mapper;

import com.guille.media.reproductor.uploader.storage.app.commands.requests.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.requests.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;
import com.guille.media.reproductor.uploader.storage.app.converters.InstantToString;
import com.guille.media.reproductor.uploader.storage.app.converters.StorageKeyToString;
import com.guille.media.reproductor.uploader.storage.app.converters.StringToMimeType;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.StreamingRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.UploadRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.StreamingSessionResponse;
import com.guille.media.reproductor.uploader.storage.presenter.dto.response.UploadResponse;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {
                StorageKeyToString.class,
                InstantToString.class,
                StringToMimeType.class,
})
public interface UploadMapper {
        CreateUploadCommand toUploadCommand(UploadRequest uploadRequest);

        UploadResponse toUploadResponse(UploadSession uploadResponse);

        StreamingCommand toStreamingCommand(StreamingRequest streamingRequest);

        // @Mapping(target = "storageKey", source = "storageKey.key")
        StreamingSessionResponse toStreamingSessionResponse(StreamingSession streamingSession);
}
