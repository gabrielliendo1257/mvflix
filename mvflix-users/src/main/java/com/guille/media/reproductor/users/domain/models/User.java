package com.guille.media.reproductor.users.domain.models;

import java.util.Objects;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * Usuario del sistema.
 *
 * <p>El dominio de users solo mantiene la política: identidad, plan y estado. La cuota y el
 * consumo de almacenamiento son responsabilidad del storage-service (fuente de verdad), que es
 * quien reserva, libera y registra el uso de forma atómica con el límite derivado del plan.
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

  /** Nombre a mostrar en la UI; null = aún no definido (la UI usa username). */
  private String displayName;

  /** Avatar del perfil; null = sin imagen (la UI muestra iniciales). */
  private String avatarUrl;

  public static User createNew(Username username, Email email) {
    return new User(new UserId(UUID.randomUUID()), username, email, Plan.FREE, true, 0,
        null, null);
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
