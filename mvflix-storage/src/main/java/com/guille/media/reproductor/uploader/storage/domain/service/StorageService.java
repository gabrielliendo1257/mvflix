package com.guille.media.reproductor.uploader.storage.domain.service;

import com.guille.media.reproductor.uploader.storage.app.commands.requets.CreateUploadCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.requets.StreamingCommand;
import com.guille.media.reproductor.uploader.storage.app.commands.response.StreamingSession;
import com.guille.media.reproductor.uploader.storage.app.commands.response.UploadSession;

public interface StorageService {
	UploadSession createUploadSession(CreateUploadCommand command);

	StreamingSession generateStreamingSession(StreamingCommand command);

	void completeUpload(String uploadId);
}
