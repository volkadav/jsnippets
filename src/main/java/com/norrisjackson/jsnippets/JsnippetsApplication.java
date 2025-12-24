package com.norrisjackson.jsnippets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories("com.norrisjackson.jsnippets.data")
@EntityScan("com.norrisjackson.jsnippets.data")
@EnableScheduling
public class JsnippetsApplication {
	public static void main(String[] args) {
		SpringApplication.run(JsnippetsApplication.class, args);
	}
}
