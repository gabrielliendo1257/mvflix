package com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer;

import org.mapstruct.Mapper;

import com.gcorp.service.app.authorizationservice.models.Role;

@Mapper(componentModel = "spring", uses = {AuthorityMapper.class})
public interface RoleMapper {
    Role toModel(RoleEntity roleEntity);
}
