package gcorp.microservicesarm.app.mvflix_users.domain.ports;

import gcorp.microservicesarm.app.mvflix_users.domain.models.User;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<Void> reserveStorage(long bytes, String userId);

	Mono<User> createStorageByNewUsers(String username, String email);

	Mono<User> getMe();
}
