package com.guille.mvflix.devseed;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades {@code devseed.users} cargadas desde {@code dev-users.yaml}.
 */
@ConfigurationProperties(prefix = "devseed")
public class DevUserSeedProperties {

    private List<DevUser> users = new ArrayList<>();

    public List<DevUser> getUsers() {
        return this.users;
    }

    public void setUsers(List<DevUser> users) {
        this.users = users;
    }
}
