package com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer;

import org.mapstruct.Mapper;

import com.gcorp.service.app.authorizationservice.models.CustomerSecurity;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerSecurity toSecurityAccount(CustomerEntity customerEntity);
}
