package com.gcorp.service.app.authorizationservice.erros;

public class RoleHasEmptyAuthorizations extends RuntimeException {

    public RoleHasEmptyAuthorizations(String message) {
        super(message);
    }
}
