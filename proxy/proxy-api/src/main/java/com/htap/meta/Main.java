package com.htap.meta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application Spring Boot pour exposer le proxy HTAP via HTTP.
 *
 * Cette application démarre un serveur web embarqué et
 * expose des endpoints REST qui délèguent au QueryRouter existant.
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
