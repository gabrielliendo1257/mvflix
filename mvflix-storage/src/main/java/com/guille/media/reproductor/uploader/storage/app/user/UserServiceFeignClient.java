package com.guille.media.reproductor.uploader.storage.app.user;

import com.guille.media.reproductor.uploader.storage.infrastructure.http.interceptors.FeignAuthenticationInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "user-service", configuration = FeignAuthenticationInterceptor.class)
public interface UserServiceFeignClient {

    @PostMapping(value = "/")
    void applyQuota(String subject, Long quota);
}
