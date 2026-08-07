package com.guille.media.reproductor.uploader.storage.app.user;

public class FakeUserServiceQueryPort implements UserServiceCommandPort {
    @Override
    public void applyQuota(String subject, Long quota) {
    }
}
