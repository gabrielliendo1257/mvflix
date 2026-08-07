package com.guille.media.reproductor.uploader.utils.enums;

import java.util.List;

public enum Roles {
    ADMIN(List.of(Permissions.ADMININTRATOR, Permissions.SUPER_USER, Permissions.LIST_BUILDER,
            Permissions.PREMIUM_CONTENT, Permissions.STANDARD_CONTENT, Permissions.CONTENT_DIRECTOR,
            Permissions.PLATFORM_MANAGER)),
    PREMIUM_USER(List.of(Permissions.LIST_BUILDER, Permissions.PREMIUM_CONTENT, Permissions.GUEST,
            Permissions.STANDARD_CONTENT)),
    STANDARD_USER(List.of(Permissions.LIST_BUILDER, Permissions.GUEST, Permissions.STANDARD_CONTENT)),
    GUEST_USER(List.of(Permissions.GUEST, Permissions.STANDARD_CONTENT));

    private List<Permissions> authorities;

    Roles(List<Permissions> authorities) {
        this.authorities = authorities;
    }

    public List<Permissions> getPermissions() {
        return this.authorities;
    }
}
