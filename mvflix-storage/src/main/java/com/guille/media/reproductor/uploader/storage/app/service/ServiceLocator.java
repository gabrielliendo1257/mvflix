package com.guille.media.reproductor.uploader.storage.app.service;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class ServiceLocator {

    private final DiscoveryClient discoveryClient;

    public ServiceLocator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public URI authorizationServer() {
        ServiceInstance instance = discoveryClient
                .getInstances("authorization-service")
                .get(0);

        return instance.getUri();
    }
}
