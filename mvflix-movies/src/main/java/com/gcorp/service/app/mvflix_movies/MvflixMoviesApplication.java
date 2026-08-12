package com.gcorp.service.app.mvflix_movies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MvflixMoviesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MvflixMoviesApplication.class, args);
	}

}
