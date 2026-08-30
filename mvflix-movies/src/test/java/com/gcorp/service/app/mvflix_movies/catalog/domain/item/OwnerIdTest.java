package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OwnerIdTest {

    @Test
    void keepsTheOwnerValue() {
        assertThat(OwnerId.of("Javier").value()).isEqualTo("Javier");
    }

    @Test
    void rejectsMissingOwner() {
        assertThatThrownBy(() -> OwnerId.of(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
