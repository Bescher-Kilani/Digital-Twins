package org.DigiTwinStudio.DigiTwin_Backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DigiTwinBackendApplication {
	public static void main(String[] args) {
		try {
			// Try to load .env file for local development
			Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
			
			// Only set properties if they're available in .env and not already set as environment variables
			if (dotenv.get("APPLICATION_NAME") != null && System.getenv("APPLICATION_NAME") == null) {
				System.setProperty("spring.application.name", dotenv.get("APPLICATION_NAME"));
			}
			if (dotenv.get("MONGODB_URI") != null && System.getenv("MONGODB_URI") == null) {
				System.setProperty("spring.data.mongodb.uri", dotenv.get("MONGODB_URI"));
			}
			if (dotenv.get("MONGODB_DATABASE") != null && System.getenv("MONGODB_DATABASE") == null) {
				System.setProperty("spring.data.mongodb.database", dotenv.get("MONGODB_DATABASE"));
			}
		} catch (Exception e) {
			// .env file not found, continue without it (useful for Docker environments)
			System.out.println("No .env file found, using environment variables and application.properties");
		}

		SpringApplication.run(DigiTwinBackendApplication.class, args);
	}
}
