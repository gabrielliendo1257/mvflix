package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.UploadSession;
import com.guille.media.reproductor.uploader.storage.managedstorage.application.command.response.UploadSummary;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.mapping.InstantToString;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.mapping.StorageKeyToString;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.mapping.StringToMimeType;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.StreamingRequest;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadRequest;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.StreamingSessionResponse;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadResponse;
import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web.UploadSummaryResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {
                StorageKeyToString.class,
                InstantToString.class,
                StringToMimeType.class,
})
public interface UploadMapper {
        CreateUploadCommand toUploadCommand(UploadRequest uploadRequest);

        @Mapping(target = "status", source = "currentStatus")
        UploadResponse toUploadResponse(UploadSession uploadResponse);

        StreamingCommand toStreamingCommand(StreamingRequest streamingRequest);

        // @Mapping(target = "storageKey", source = "storageKey.key")
        StreamingSessionResponse toStreamingSessionResponse(StreamingSession streamingSession);

        UploadSummaryResponse toUploadSummaryResponse(UploadSummary uploadSummary);
}
