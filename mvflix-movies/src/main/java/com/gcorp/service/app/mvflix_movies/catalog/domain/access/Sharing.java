package com.gcorp.service.app.mvflix_movies.catalog.domain.access;

import java.util.Set;

/** Immutable set of users with whom an item is shared. */
public final class Sharing {
    private static final Sharing EMPTY = new Sharing(Set.of());
    private final Set<String> users;

    private Sharing(Set<String> users) {
        this.users = Set.copyOf(users);
    }

    public static Sharing empty() {
        return EMPTY;
    }

    public static Sharing of(Set<String> users) {
        return users == null || users.isEmpty() ? EMPTY : new Sharing(users);
    }

    public Set<String> users() {
        return users;
    }

    public boolean contains(String username) {
        return users.contains(username);
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    public int size() {
        return users.size();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Sharing sharing && users.equals(sharing.users);
    }

    @Override
    public int hashCode() {
        return users.hashCode();
    }

    @Override
    public String toString() {
        return users.toString();
    }
}
