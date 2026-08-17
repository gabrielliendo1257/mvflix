package com.gcorp.service.app.mvflix_movies;

import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TmdbProperties.class)
public class MvflixMoviesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MvflixMoviesApplication.class, args);
	}

}
