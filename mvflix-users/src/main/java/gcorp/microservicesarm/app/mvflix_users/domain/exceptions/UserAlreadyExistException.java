package gcorp.microservicesarm.app.mvflix_users.domain.exceptions;

import lombok.Getter;

@Getter
public class UserAlreadyExistException extends RuntimeException {
    private final String username;

    public UserAlreadyExistException(String username) {
        this.username = username;
    }
}
