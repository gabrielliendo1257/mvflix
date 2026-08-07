package gcorp.microservicesarm.app.mvflix_users.app.errors;

public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String args) {
        super(args);
    }
}
