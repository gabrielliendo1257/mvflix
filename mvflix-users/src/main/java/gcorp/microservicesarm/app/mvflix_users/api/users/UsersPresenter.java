package gcorp.microservicesarm.app.mvflix_users.api.users;

import gcorp.microservicesarm.app.mvflix_users.api.dto.request.UserData;
import gcorp.microservicesarm.app.mvflix_users.api.dto.response.UserResponse;
import gcorp.microservicesarm.app.mvflix_users.domain.ports.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@Slf4j
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequiredArgsConstructor
public class UsersPresenter {

    private final UserService userService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> createNewStorage(@RequestBody UserData userData) {
        return this.userService
                .createStorageByNewUsers(userData.username(), userData.email())
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/me")
    public Mono<ResponseEntity<?>> me() {
        return this.userService.getMe().map(user -> ResponseEntity.ok(UserResponse.from(user)));
    }
}
