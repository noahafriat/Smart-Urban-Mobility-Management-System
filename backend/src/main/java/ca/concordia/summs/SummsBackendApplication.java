package ca.concordia.summs;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@org.springframework.scheduling.annotation.EnableAsync
@SpringBootApplication
public class SummsBackendApplication {

	public static void main(String[] args) {
		loadLocalDotEnv();
		SpringApplication.run(SummsBackendApplication.class, args);
	}

	/**
	 * Loads the first existing {@code .env} from the working directory or {@code backend/.env}
	 * (repo root vs. {@code backend} as cwd) into system properties so {@code application.properties}
	 * can use {@code ${STM_API_KEY}}. Real environment variables win.
	 */
	private static void loadLocalDotEnv() {
		Path[] candidates = new Path[] { Path.of(".env"), Path.of("backend", ".env") };
		for (Path relative : candidates) {
			Path abs = relative.toAbsolutePath().normalize();
			if (!Files.isRegularFile(abs)) {
				continue;
			}
			Dotenv dotenv = Dotenv.configure()
					.directory(abs.getParent().toString())
					.filename(abs.getFileName().toString())
					.ignoreIfMissing()
					.load();
			dotenv.entries().forEach(e -> {
				String key = e.getKey();
				if (System.getenv(key) == null && System.getProperty(key) == null) {
					System.setProperty(key, e.getValue());
				}
			});
			return;
		}
	}

}
