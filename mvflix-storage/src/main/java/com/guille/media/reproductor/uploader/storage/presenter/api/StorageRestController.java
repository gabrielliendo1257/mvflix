package com.guille.media.reproductor.uploader.storage.presenter.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guille.media.reproductor.uploader.storage.domain.service.StorageService;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.StreamingRequest;
import com.guille.media.reproductor.uploader.storage.presenter.dto.request.UploadRequest;
import com.guille.media.reproductor.uploader.storage.presenter.mapper.UploadMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/v1/movie/storage", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class StorageRestController {

	private final StorageService storageService;
	private final UploadMapper uploadMapper;

	public StorageRestController(StorageService storageService, UploadMapper uploadMapper) {
		this.storageService = storageService;
		this.uploadMapper = uploadMapper;
	}

	@PostMapping(value = "/upload")
	public ResponseEntity<?> uploadSession(@Valid @RequestBody UploadRequest uploadRequest) {
		var uploadCommand = this.uploadMapper.toUploadCommand(uploadRequest);
		var sessionCreatedResponse = this.storageService.createUploadSession(uploadCommand);
		var response = this.uploadMapper.toUploadResponse(sessionCreatedResponse);

		return ResponseEntity.ok(response);
	}

	/**
	 * Verifica dentro del bucket "uploads" la existencia de ese archivo,
	 * utilizando el uploadId
	 */
	@PostMapping(value = "/upload/{uploadId}/complete")
	public ResponseEntity<?> completeUpload(@PathVariable String uploadId) {
		this.storageService.completeUpload(uploadId);
		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/streaming")
	public ResponseEntity<?> streaming(@Valid @RequestBody StreamingRequest streamingRequest) {
		var streamingrequest = this.uploadMapper.toStreamingCommand(streamingRequest);
		var createdStreamingResponse = this.storageService.generateStreamingSession(streamingrequest);
		var response = this.uploadMapper.toStreamingSessionResponse(createdStreamingResponse);

		return ResponseEntity.ok(response);
	}
}
