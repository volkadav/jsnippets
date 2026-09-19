package com.norrisjackson.jsnippets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    DataRedisAutoConfiguration.class,
    DataRedisRepositoriesAutoConfiguration.class
})
@EnableJpaRepositories("com.norrisjackson.jsnippets.data")
@EntityScan("com.norrisjackson.jsnippets.data")
@EnableScheduling
public class JsnippetsApplication {
	public static void main(String[] args) {
		SpringApplication.run(JsnippetsApplication.class, args);
	}
}
