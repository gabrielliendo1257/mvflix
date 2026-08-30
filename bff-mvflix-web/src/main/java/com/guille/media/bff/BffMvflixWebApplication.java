package com.guille.media.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BffMvflixWebApplication {

  public static void main(String[] args) {
    SpringApplication.run(BffMvflixWebApplication.class, args);
  }
}
