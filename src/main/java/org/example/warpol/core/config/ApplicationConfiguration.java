package org.example.warpol.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public Random random() {
        return new Random();
    }

}
