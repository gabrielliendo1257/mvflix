package gcorp.microservicesarm.app.mvflix_users.api.dto.response;

import gcorp.microservicesarm.app.mvflix_users.domain.models.User;

public record UserResponse(String id, String username, String email, String plan, boolean enabled) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().value().toString(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getPlan().name(),
                user.isEnabled());
    }
}
