package codes.thischwa.bcg;

import codes.thischwa.bcg.service.BirthdayScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;


/**
 * The main application class for the Birthday Calendar Generator (BCG) application.
 *
 * <p>This class acts as the entry point for running the Spring Boot application. It starts the
 * application with a non-web configuration and handles any unexpected exceptions during startup.
 *
 * <p>The application runner bean provided in this class includes special functionality to stop the
 * application after a single invocation of the birthday calendar synchronization process, in case
 * the `run-once` argument is detected.
 */
@ConfigurationPropertiesScan
@SpringBootApplication
@Slf4j
public class BcgApp {

    public static void main(String[] args) {
        try {
            new SpringApplicationBuilder(BcgApp.class)
                    .web(WebApplicationType.NONE)
                    .run(args);
        } catch (Exception e) {
            log.error("Unexpected exception, Spring Boot stops! Message: {}", e.getMessage());
            System.exit(10);
        }
    }

    /**
     * Adds a command-line option to stop the application after calendar synchronization.
     */
    @Bean
    @Profile("!test")
    public ApplicationRunner applicationRunner(BirthdayScheduler birthdayScheduler) {
        return args -> {
            if (args.containsOption("run-once")) {
                log.info("Argument '--run-once' detected. Starting calendar synchronization...");
                birthdayScheduler.process(); // Trigger calendar sync
                log.info("Calendar synchronization complete (run-once). Shutting down application.");
                System.exit(0);
            }
        };
    }
}