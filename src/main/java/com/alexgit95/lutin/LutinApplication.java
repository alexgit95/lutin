package com.alexgit95.lutin;

import com.alexgit95.lutin.config.CloudflareProperties;
import com.alexgit95.lutin.config.LutinProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({LutinProperties.class, CloudflareProperties.class})
public class LutinApplication {

    public static void main(String[] args) {
        SpringApplication.run(LutinApplication.class, args);
    }
}
