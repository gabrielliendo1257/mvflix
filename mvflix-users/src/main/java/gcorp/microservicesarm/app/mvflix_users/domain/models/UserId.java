package gcorp.microservicesarm.app.mvflix_users.domain.models;

import java.util.UUID;

import gcorp.microservicesarm.app.mvflix_users.app.errors.InvalidUserIdException;
import gcorp.microservicesarm.app.mvflix_users.domain.exceptions.EmptyVaribleException;

public record UserId(UUID value) {
    public UserId {
        if(value == null)
            throw new EmptyVaribleException("El email no puede ser null.", "username");
    }

    public static UserId from(String value) {
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserIdException("UUID no valido.");
        }
    }
}
