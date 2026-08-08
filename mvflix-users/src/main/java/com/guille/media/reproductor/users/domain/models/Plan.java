package com.guille.media.reproductor.users.domain.models;

public enum Plan {
    FREE(0),
    PRO(1),
    ENTERPRISE(2);

    private final int rank;

    Plan(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
