package com.gcorp.service.app.authorizationservice;

import java.time.Instant;
import java.util.Set;

import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.AuthorityEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.AuthorityJpaRepository;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerRepository;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.RoleEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.RoleJpaRepository;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class AuthorizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServiceApplication.class, args);
    }

    @Bean
    @Profile("!prod")
    ApplicationRunner seedDevData(
            CustomerRepository customerRepository,
            RoleJpaRepository roleJpaRepository,
            AuthorityJpaRepository authorityJpaRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            var readMovie = authorityJpaRepository.findByName("READ_MOVIE")
                    .orElseGet(() -> authorityJpaRepository.save(new AuthorityEntity("READ_MOVIE")));
            var deleteMovie = authorityJpaRepository.findByName("DELETE_MOVIE")
                    .orElseGet(() -> authorityJpaRepository.save(new AuthorityEntity("DELETE_MOVIE")));

            var customerRole = roleJpaRepository.findByRoleName("CUSTOMER")
                    .orElseGet(() -> roleJpaRepository.save(new RoleEntity("CUSTOMER", Instant.now(), Set.of(readMovie))));
            var adminRole = roleJpaRepository.findByRoleName("ADMIN")
                    .orElseGet(() -> roleJpaRepository.save(new RoleEntity("ADMIN", Instant.now(), Set.of(readMovie, deleteMovie))));

            if (customerRepository.findByUsername("Javier").isEmpty()) {
                customerRepository.save(new CustomerEntity("Javier", passwordEncoder.encode("JavierPassword"), customerRole));
            }
            if (customerRepository.findByUsername("Admin").isEmpty()) {
                customerRepository.save(new CustomerEntity("Admin", passwordEncoder.encode("AdminPassword"), adminRole));
            }

            var accountSec = customerRepository.findSecurityCustomerByUsername("Javier").orElseThrow();

            log.info("Customer: {} role: {}", accountSec.getUsername(),
                    accountSec.getRole().getRoleName());
            accountSec.getRole()
                    .getAuthorities()
                    .stream()
                    .forEach(c -> {
                        log.info("Authority: {}", c.getName());
                    });
        };
    }
}
