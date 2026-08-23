package com.guille.media.bff.experience.addmedia.model;

import java.util.UUID;

/** Identificador del proceso de alta de contenido propiedad del BFF. */
public record AddMediaId(String value) {

  public AddMediaId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("AddMediaId no puede ser vacío");
    }
  }

  public static AddMediaId newId() {
    return new AddMediaId(UUID.randomUUID().toString());
  }

  @Override
  public String toString() {
    return this.value;
  }
}
