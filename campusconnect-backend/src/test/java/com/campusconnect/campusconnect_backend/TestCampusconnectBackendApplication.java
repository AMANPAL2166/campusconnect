package com.campusconnect.campusconnect_backend;

import org.springframework.boot.SpringApplication;

public class TestCampusconnectBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(CampusconnectBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
