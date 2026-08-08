package com.guille.media.reproductor.uploader.storage.app.commands.requests;

import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;

public record CreateUploadCommand(String filename, long size, MimeType mimeType) {}
