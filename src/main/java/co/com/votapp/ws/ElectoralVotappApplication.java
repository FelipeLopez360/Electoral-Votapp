package co.com.votapp.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication already triggers @ComponentScan on the co.com.votapp.ws base package,
// which detects all @UseCase beans (a @Component meta-annotation) across all modules automatically.
// No explicit scanBasePackages needed.
@SpringBootApplication
public class ElectoralVotappApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElectoralVotappApplication.class, args);
	}

}
