package com.gcorp.service.app.authorizationservice;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.AuthorityEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.AuthorityJpaRepository;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerMapper;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerRepository;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.RoleEntity;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.RoleJpaRepository;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class AuthorizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner persistMovies(
            CustomerRepository customerRepository,
            RoleJpaRepository roleJpaRepository,
            AuthorityJpaRepository authorityJpaRepository,
            PasswordEncoder passwordEncoder,
            CustomerMapper customerMapperImpl) {
        return args -> {
            var readMovie = new AuthorityEntity("READ_MOVIE");
            var deleteMovie = new AuthorityEntity("DELETE_MOVIE");

            authorityJpaRepository.saveAll(
                    List.of(readMovie, deleteMovie));

            var customerRole = new RoleEntity(
                    "CUSTOMER",
                    Instant.now(),
                    Set.of(readMovie));

            var adminRole = new RoleEntity(
                    "ADMIN",
                    Instant.now(),
                    Set.of(readMovie, deleteMovie));

            roleJpaRepository.saveAll(
                    List.of(customerRole, adminRole));

            var cusJa = customerRepository
                    .save(new CustomerEntity("Javier", passwordEncoder.encode("JavierPassword"), roleJpaRepository.findById(1).orElseThrow()));
            var cusAd = customerRepository
                    .save(new CustomerEntity("Admin", passwordEncoder.encode("AdminPassword"), roleJpaRepository.findById(2).orElseThrow()));

            var accountSec = customerRepository.findSecurityCustomerById(2).orElseThrow();

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
