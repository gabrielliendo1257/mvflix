package com.guille.media.reproductor.uploader.storage.app.user;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!sandbox")
@RequiredArgsConstructor
public class UserServiceQueryPortImpl implements UserServiceCommandPort {
    private final UserServiceFeignClient userServiceFeignClient;

    public void applyQuota(String subject, Long quota) {
        this.userServiceFeignClient.applyQuota(subject, quota);
    }
}
