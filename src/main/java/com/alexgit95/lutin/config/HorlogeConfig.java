package com.alexgit95.lutin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class HorlogeConfig {

    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
