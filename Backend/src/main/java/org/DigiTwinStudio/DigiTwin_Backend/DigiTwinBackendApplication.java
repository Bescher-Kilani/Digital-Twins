package org.DigiTwinStudio.DigiTwin_Backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DigiTwinBackendApplication {
	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();

		System.setProperty("spring.application.name", dotenv.get("APPLICATION_NAME"));
		System.setProperty("spring.data.mongodb.uri", dotenv.get("MONGODB_URI"));
		System.setProperty("spring.data.mongodb.database", dotenv.get("MONGODB_DATABASE"));

		SpringApplication.run(DigiTwinBackendApplication.class, args);
	}
}
