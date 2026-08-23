package com.guille.media.bff.experience.addmedia.model;

/** Transición no permitida entre fases del proceso Add Media. */
public class InvalidAddMediaTransition extends RuntimeException {

  private final AddMediaPhase current;
  private final AddMediaPhase target;

  public InvalidAddMediaTransition(AddMediaPhase current, AddMediaPhase target) {
    super("Transición inválida del proceso Add Media: " + current + " -> " + target);
    this.current = current;
    this.target = target;
  }

  public AddMediaPhase current() {
    return this.current;
  }

  public AddMediaPhase target() {
    return this.target;
  }
}
