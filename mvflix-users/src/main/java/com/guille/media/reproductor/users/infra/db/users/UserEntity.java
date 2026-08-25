package com.guille.media.reproductor.users.infra.db.users;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

@Data
@Table("users")
public class UserEntity {

    @Id
    private UUID id;
    private final String username;
    private final String email;
    private final String plan;
    private final boolean enabled;
    private int violations;
    private String displayName;
    private String avatarUrl;
    private final Instant createdAt;
    private final Instant updatedAt;
}