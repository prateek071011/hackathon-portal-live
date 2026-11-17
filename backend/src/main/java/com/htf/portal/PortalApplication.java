package com.htf.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the backend. Starting this class starts the embedded web server,
 * runs schema.sql against MySQL and registers every @Component / @Service / @RestController.
 */
@SpringBootApplication
public class PortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortalApplication.class, args);
    }
}
