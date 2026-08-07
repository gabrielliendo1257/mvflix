package gcorp.microservicesarm.app.mvflix_users.domain.ports;

import gcorp.microservicesarm.app.mvflix_users.domain.models.User;
import gcorp.microservicesarm.app.mvflix_users.domain.models.UserId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SimpleUserRepository {
    Mono<User> save(User user);
    Flux<User> findAll();
    Mono<User> update(User user);
    Mono<User> findById(UserId userId);
	Mono<User> findByUsername(String username);
}
