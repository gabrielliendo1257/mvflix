package com.guille.mvflix.devseed;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Aplica cada {@link DevUserSeeder} del classpath para cada usuario definido en
 * {@code dev-users.yaml}. Idempotente por diseño: cada seeder decide si el
 * usuario ya existe y omite la creacion.
 */
@Slf4j
public class DevSeedRunner implements ApplicationRunner {

    private final List<DevUserSeeder> seeders;

    private final DevUserSeedProperties properties;

    public DevSeedRunner(List<DevUserSeeder> seeders, DevUserSeedProperties properties) {
        this.seeders = seeders;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (DevUser user : this.properties.getUsers()) {
            log.info("Dev seed: usuario {} definido, {} seeders aplicables", user.getUsername(), this.seeders.size());
            for (DevUserSeeder seeder : this.seeders) {
                try {
                    seeder.seed(user);
                    log.info("Dev seed aplicado: seeder={}, username={}", seeder.getClass().getSimpleName(), user.getUsername());
                } catch (Exception error) {
                    log.warn("Dev seed fallo (se continua el arranque): seeder={}, username={}, cause={}",
                            seeder.getClass().getSimpleName(), user.getUsername(), error.getMessage());
                }
            }
        }
    }
}