package com.gcorp.service.app.mvflix_media_ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication @EnableScheduling
public class MvflixMediaIngestionApplication {
  public static void main(String[] args) { SpringApplication.run(MvflixMediaIngestionApplication.class, args); }
}
