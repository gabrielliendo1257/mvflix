package com.guille.media.reproductor.users.domain.models;

import java.util.Objects;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * Usuario del sistema.
 *
 * <p>El dominio de users solo mantiene la política: identidad, plan y estado. El límite de
 * almacenamiento se deriva del {@link Plan} y el uso real (bytes consumidos) es responsabilidad del
 * storage-service, que es quien reserva, libera y registra el consumo de forma atómica.
 */
@Data
@ToString
@AllArgsConstructor
public class User {
  /** A partir de este número de violaciones el usuario queda bloqueado para subidas. */
  public static final int VIOLATION_THRESHOLD = 3;

  private UserId id;
  private Username username;
  private Email email;
  private Plan plan;
  private boolean enabled;
  private int violations;

  public static User createNew(Username username, Email email) {
    return new User(new UserId(UUID.randomUUID()), username, email, Plan.FREE, true, 0);
  }

  /**
   * @return cuota asignada según el {@link Plan} actual.
   */
  public StorageQuota quota() {
    return StorageQuota.getQuota(this.plan);
  }

  public void changePlan(Plan plan) {
    this.plan = Objects.requireNonNull(plan);
  }

  /** Registra una infracción (subida maliciosa o inconsistente) y devuelve si quedó bloqueado. */
  public boolean registerViolation() {
    this.violations++;
    return isBlocked();
  }

  public boolean isBlocked() {
    return this.violations >= VIOLATION_THRESHOLD;
  }
}
