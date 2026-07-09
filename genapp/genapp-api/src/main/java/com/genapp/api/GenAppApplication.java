package com.genapp.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Application Class untuk GenApp API
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.genapp.api", "com.genapp.core"})
public class GenAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(GenAppApplication.class, args);
    }
}
