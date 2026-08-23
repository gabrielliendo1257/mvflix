package com.guille.media.reproductor.uploader.storage.managedstorage.application.command.request;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.MimeType;

public record CreateUploadCommand(String filename, long size, MimeType mimeType) {}
