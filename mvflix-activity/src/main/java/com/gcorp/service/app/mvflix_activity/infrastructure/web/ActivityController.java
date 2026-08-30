package com.gcorp.service.app.mvflix_activity.infrastructure.web;

import com.gcorp.service.app.mvflix_activity.application.ActivityQueryService;
import com.gcorp.service.app.mvflix_activity.application.ActivityQueryService.ActivityRecord;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/api/v1/activity")
public class ActivityController {
  private final ActivityQueryService service;
  public ActivityController(ActivityQueryService service) { this.service=service; }
  @GetMapping("/history") public Flux<ActivityRecord> history(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="20") @Min(1) @Max(100) int limit){return service.history(owner(jwt),limit);}
  @GetMapping("/continue-watching") public Flux<ActivityRecord> continueWatching(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="20") @Min(1) @Max(100) int limit){return service.continueWatching(owner(jwt),limit);}
  @GetMapping("/movies/{movieId}") public Mono<ActivityRecord> movie(@AuthenticationPrincipal Jwt jwt,@PathVariable @Positive long movieId){return service.movie(owner(jwt),movieId);}
  private String owner(Jwt jwt) { String value=jwt.getClaimAsString("preferred_username"); if(value==null||value.isBlank())value=jwt.getClaimAsString("name"); return value==null||value.isBlank()?jwt.getSubject():value; }
}
