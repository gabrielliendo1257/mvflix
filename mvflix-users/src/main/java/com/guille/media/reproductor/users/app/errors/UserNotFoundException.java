package com.guille.media.reproductor.users.app.errors;

public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String args) {
        super(args);
    }
}
