package com.bdsystems.builder_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@RequestMapping("/")
public class ApiApplication {

	static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}
}
