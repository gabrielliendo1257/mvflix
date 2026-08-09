package com.guille.media.bff.app.service;

import com.guille.media.bff.app.dto.UploadListItem;
import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.dto.QuotaSnapshot;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.app.ports.UsersWebPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WebHomeService {

  private final UsersWebPort usersWebClient;
  private final StorageWebClient storageWebClient;

public Mono<HomeView> home(String bearerToken) {
    Mono<UserProfile> me = this.usersWebClient.me(bearerToken);
    Mono<QuotaSnapshot> quota = this.storageWebClient.quota(bearerToken);
    Mono<List<UploadListItem>> uploads =
        this.storageWebClient.listUploads(bearerToken, 10).collectList();

    return Mono.zip(me, quota)
        .flatMap(pair -> uploads.map(items -> new HomeView(pair.getT1(), pair.getT2(), items)));
  }

  public record HomeView(
      UserProfile profile, QuotaSnapshot quota, List<UploadListItem> recentUploads) {}
}