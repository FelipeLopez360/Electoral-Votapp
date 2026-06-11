package co.com.votapp.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication triggers @ComponentScan on the co.com.votapp.ws base package,
// detecting all @Component, @Service, @Repository, and @Controller beans.
// Domain use cases (domain/usecase/) have NO Spring annotations — they are wired
// manually via @Configuration in config/DomainConfig.java (Hexagonal Architecture).
@SpringBootApplication
@EnableScheduling
public class ElectoralVotappApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElectoralVotappApplication.class, args);
	}

}
