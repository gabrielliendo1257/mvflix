package com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Integer> {
    Optional<CustomerEntity> findByUsername(String username);

    @Query("""
                select c
                from CustomerEntity c
                join fetch c.role r
                left join fetch r.authorities
                where c.id = :id
            """)
    Optional<CustomerEntity> findSecurityCustomerById(Integer id);

    @Query("""
                select c
                from CustomerEntity c
                join fetch c.role r
                left join fetch r.authorities
                where c.username = :username
            """)
    Optional<CustomerEntity> findSecurityCustomerByUsername(String username);
}
