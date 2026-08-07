package gcorp.microservicesarm.app.mvflix_users.app.errors;

public class UnAuthorizedException extends RuntimeException {
    public UnAuthorizedException(String message) {
        super(message);
    }
}
