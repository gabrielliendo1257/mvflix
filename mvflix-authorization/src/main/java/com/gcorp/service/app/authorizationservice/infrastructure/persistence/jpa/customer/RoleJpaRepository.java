package com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleEntity, Integer> {

}
