package com.guille.media.reproductor.uploader.storage.app.service;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
public class ServiceLocator {

    private final DiscoveryClient discoveryClient;

    public ServiceLocator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public URI authorizationServer() {
        return this.getInstance("authorization-service");
    }

    private URI getInstance(String serviceId) {
        List<ServiceInstance> instances = this.discoveryClient.getInstances(serviceId);

        if (instances.isEmpty()) {
            throw new IllegalStateException(
                    "No instances registered for service: " + serviceId);
        }

        return instances.get(0).getUri();
    }
}
