package gcorp.microservicesarm.app.mvflix_users.app.errors;

public class InvalidUserIdException extends RuntimeException {

    public InvalidUserIdException(String message) {
        super(message);
    }
}
