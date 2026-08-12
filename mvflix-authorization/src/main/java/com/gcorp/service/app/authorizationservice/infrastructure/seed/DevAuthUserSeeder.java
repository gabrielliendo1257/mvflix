package com.gcorp.service.app.authorizationservice.infrastructure.seed;

import java.time.Instant;
import java.util.Set;

import com.guille.mvflix.devseed.DevUser;
import com.guille.mvflix.devseed.DevUserSeeder;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.AuthorityEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.AuthorityJpaRepository;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerRepository;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.RoleEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.RoleJpaRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seed dev: materializa en el IdP a los usuarios definidos en
 * {@code dev-users.yaml} (password y rol incluidos), reutilizando el bootstrap
 * de roles/authorities de {@code seedDevData}.
 */
@Slf4j
@Component
@Profile("dev")
public class DevAuthUserSeeder implements DevUserSeeder {

    private final CustomerRepository customerRepository;

    private final RoleJpaRepository roleJpaRepository;

    private final AuthorityJpaRepository authorityJpaRepository;

    private final PasswordEncoder passwordEncoder;

    public DevAuthUserSeeder(
            CustomerRepository customerRepository,
            RoleJpaRepository roleJpaRepository,
            AuthorityJpaRepository authorityJpaRepository,
            PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.authorityJpaRepository = authorityJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void seed(DevUser devUser) {
        if (this.customerRepository.findByUsername(devUser.getUsername()).isPresent()) {
            log.info("Dev customer already exists: {}", devUser.getUsername());
            return;
        }

        var readMovie = this.authorityJpaRepository.findByName("READ_MOVIE")
                .orElseGet(() -> this.authorityJpaRepository.save(new AuthorityEntity("READ_MOVIE")));
        var role = this.roleJpaRepository.findByRoleName(devUser.getRole())
                .orElseGet(() -> this.roleJpaRepository.save(
                        new RoleEntity(devUser.getRole(), Instant.now(), Set.of(readMovie))));

        this.customerRepository.save(new CustomerEntity(
                devUser.getUsername(),
                this.passwordEncoder.encode(devUser.getPassword()),
                role));
        log.info("Dev customer provisioned: {}, role={}", devUser.getUsername(), devUser.getRole());
    }
}