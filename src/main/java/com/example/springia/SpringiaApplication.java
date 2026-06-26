package com.example.springia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringiaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringiaApplication.class, args);
	}

}
