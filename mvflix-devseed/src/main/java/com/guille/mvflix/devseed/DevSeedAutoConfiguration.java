package com.guille.mvflix.devseed;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * Auto-configuracion del seed dev: carga {@code dev-users.yaml} (solo con el
 * perfil {@code dev}) y corre los {@link DevUserSeeder} disponibles.
 */
@AutoConfiguration
@Profile("dev")
@EnableConfigurationProperties(DevUserSeedProperties.class)
@PropertySource(value = "classpath:dev-users.yaml", factory = YamlPropertySourceFactory.class)
@ConditionalOnBean(DevUserSeeder.class)
public class DevSeedAutoConfiguration {

    @Bean
    DevSeedRunner devSeedRunner(List<DevUserSeeder> seeders, DevUserSeedProperties properties) {
        return new DevSeedRunner(seeders, properties);
    }
}