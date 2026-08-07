package com.guille.media.reproductor.uploader.storage.app.commands.requets;

import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;

public record CreateUploadCommand(String filename, long size, MimeType mimeType) {}
