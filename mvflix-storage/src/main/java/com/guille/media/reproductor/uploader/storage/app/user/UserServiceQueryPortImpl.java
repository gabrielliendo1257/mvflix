package com.guille.media.reproductor.uploader.storage.app.user;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserServiceQueryPortImpl implements UserServiceCommandPort {
    private final UserServiceFeignClient userServiceFeignClient;

    public void applyQuota(String subject, Long quota) {
        this.userServiceFeignClient.applyQuota(subject, quota);
    }
}
