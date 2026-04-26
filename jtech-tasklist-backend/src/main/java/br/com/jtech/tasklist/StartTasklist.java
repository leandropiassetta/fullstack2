package br.com.jtech.tasklist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class StartTasklist {

	public static void main(String[] args) {
		SpringApplication.run(StartTasklist.class, args);
	}

}
