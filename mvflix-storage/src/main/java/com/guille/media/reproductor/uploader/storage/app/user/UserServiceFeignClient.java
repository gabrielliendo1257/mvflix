package com.guille.media.reproductor.uploader.storage.app.user;

import com.guille.media.reproductor.uploader.storage.infrastructure.http.interceptors.FeignAuthenticationInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Profile("!sandbox")
@FeignClient(name = "user-service", url = "${services.users.url}", configuration = FeignAuthenticationInterceptor.class)
public interface UserServiceFeignClient {

    @PostMapping(value = "/")
    void applyQuota(@RequestParam String subject, @RequestParam Long quota);
}