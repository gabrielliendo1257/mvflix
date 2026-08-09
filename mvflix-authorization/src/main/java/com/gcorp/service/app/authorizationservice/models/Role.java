package com.gcorp.service.app.authorizationservice.models;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcorp.service.app.authorizationservice.erros.RoleHasEmptyAuthorizations;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role {

    private String roleName;
    private Set<Authorization> authorizations = new HashSet<>();

    @JsonCreator
    public Role(
        @JsonProperty("roleName") String roleName,
        @JsonProperty("authorizations") Set<Authorization> authorizations
    ) {
        this.roleName = roleName;
        this.authorizations = authorizations;
    }

    public Boolean containsAuthorization(Authorization authorizationName) {
        if (authorizations.isEmpty()) {
            throw new RoleHasEmptyAuthorizations(
                "Role " + this.roleName + " has empty authorizations."
            );
        }

        return this.authorizations.contains(authorizationName);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (roleName == null ? 0 : roleName.hashCode());
        result =
            prime * result +
            (authorizations == null ? 0 : authorizations.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Role other = (Role) obj;
        if (roleName == null) {
            if (other.roleName != null) return false;
        } else if (!roleName.equals(other.roleName)) return false;
        if (authorizations == null) {
            if (other.authorizations != null) return false;
        } else if (!authorizations.equals(other.authorizations)) return false;
        return true;
    }
}
