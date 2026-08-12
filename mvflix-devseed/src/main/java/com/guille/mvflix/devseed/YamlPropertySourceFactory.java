package com.guille.mvflix.devseed;

import java.io.IOException;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

/**
 * Factory que permite usar {@code @PropertySource} con archivos YAML.
 */
public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource)
            throws IOException {
        var sources =
                new YamlPropertySourceLoader()
                        .load(resource.getResource().getFilename(), resource.getResource());
        return sources.get(0);
    }
}