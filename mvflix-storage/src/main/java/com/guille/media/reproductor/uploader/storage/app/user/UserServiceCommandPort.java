package com.guille.media.reproductor.uploader.storage.app.user;

public interface UserServiceCommandPort {
    void applyQuota(String subject, Long quota);
}
