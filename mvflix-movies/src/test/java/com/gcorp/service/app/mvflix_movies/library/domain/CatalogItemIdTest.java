package com.gcorp.service.app.mvflix_movies.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CatalogItemIdTest {

    @Test
    void createsPositiveCatalogReference() {
        assertThat(CatalogItemId.of(10L).value()).isEqualTo(10L);
    }

    @Test
    void rejectsNullCatalogReference() {
        assertThatThrownBy(() -> CatalogItemId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Catalog item id must be positive");
    }

    @Test
    void rejectsNonPositiveCatalogReference() {
        assertThatThrownBy(() -> CatalogItemId.of(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Catalog item id must be positive");
    }
}
