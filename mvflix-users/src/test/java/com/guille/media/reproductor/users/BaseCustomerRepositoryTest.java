package com.guille.media.reproductor.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import com.guille.media.reproductor.users.domain.ports.SimpleUserRepository;

public abstract class BaseCustomerRepositoryTest {
    
    public abstract SimpleUserRepository getRepository();

    @Value("classpath:application.yaml")
    private Resource resource;
}
