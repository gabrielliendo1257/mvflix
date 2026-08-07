package com.gcorp.service.app.authorizationservice.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Authorization {
    String authName;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((authName == null) ? 0 : authName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Authorization other = (Authorization) obj;
        if (authName == null) {
            if (other.authName != null)
                return false;
        } else if (!authName.equals(other.authName))
            return false;
        return true;
    }
}
