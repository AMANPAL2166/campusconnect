package com.campusconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CampusconnectBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusconnectBackendApplication.class, args);
    }
}