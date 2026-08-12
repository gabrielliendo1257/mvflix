package com.guille.mvflix.devseed;

/**
 * Punto de extension por servicio: cada microservicio implementa su propia
 * forma de materializar a un {@link DevUser} en su dominio (ej: fila en su
 * tabla de usuarios, espacio de almacenamiento, credencial en el IdP).
 */
@FunctionalInterface
public interface DevUserSeeder {

    void seed(DevUser user);
}
