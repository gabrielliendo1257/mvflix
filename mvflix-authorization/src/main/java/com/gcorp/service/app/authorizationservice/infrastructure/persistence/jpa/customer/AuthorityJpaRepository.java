package com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorityJpaRepository extends JpaRepository<AuthorityEntity, Integer> {

    Optional<AuthorityEntity> findByName(String name);
}
