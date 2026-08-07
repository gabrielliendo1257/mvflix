package gcorp.microservicesarm.app.mvflix_users.domain.exceptions;

public class UserIsDisableException extends RuntimeException {
    public UserIsDisableException(String message) {
        super(message);
    }
}
