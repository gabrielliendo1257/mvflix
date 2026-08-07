package com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "roles")
@Entity
public class RoleEntity {

    @Id
    @Column(unique = true, nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "rol", unique = true, nullable = false, length = 15)
    private String roleName;

    @Column(name = "created_at", nullable = false, length = 10)
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_authorities",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    private Set<AuthorityEntity> authorities;

    @OneToMany(mappedBy = "role")
    private List<CustomerEntity> customers;

    public RoleEntity(String roleName, Instant createdAt, Set<AuthorityEntity> authorities) {
        this.roleName = roleName;
        this.createdAt = createdAt;
        this.authorities = authorities;
    }
}
