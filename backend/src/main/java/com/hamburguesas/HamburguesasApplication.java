package com.hamburguesas;

import com.hamburguesas.places.PlacesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(PlacesProperties.class)
public class HamburguesasApplication {
    public static void main(String[] args) {
        SpringApplication.run(HamburguesasApplication.class, args);
    }
}
