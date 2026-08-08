package com.guille.media.reproductor.uploader.storage.app.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("sandbox")
public class FakeUserServiceQueryPort implements UserServiceCommandPort {
    @Override
    public void applyQuota(String subject, Long quota) {
    }
}
