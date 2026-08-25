package com.guille.media.reproductor.users.api.dto.response;

import com.guille.media.reproductor.users.domain.models.User;

public record UserResponse(
    String id,
    String username,
    String displayName,
    String avatarUrl,
    String email,
    String plan,
    boolean enabled,
    int violations,
    boolean blocked) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().value().toString(),
                user.getUsername().value(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getEmail().value(),
                user.getPlan().name(),
                user.isEnabled(),
                user.getViolations(),
                user.isBlocked());
    }
}
