package gcorp.microservicesarm.app.mvflix_users.app.errors;

public class ExceededQuotaException extends RuntimeException {

    public ExceededQuotaException(String args) {
        super(args);
    }
}
