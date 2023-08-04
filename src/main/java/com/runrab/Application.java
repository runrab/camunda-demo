package com.runrab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author runrab
 */
@SpringBootApplication
//@EnableCaching
//@EnableScheduling
//@EnableAsync
public class Application {

  public static void main(String... args) {
    SpringApplication.run(Application.class, args);
  }

}