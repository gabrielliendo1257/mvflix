package com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.gcorp.service.app.authorizationservice.models.Authorization;

@Mapper(componentModel = "spring")
public interface AuthorityMapper {

    @Mapping(target = "authName", source = "name")
    Authorization toModel(AuthorityEntity entity);
}
