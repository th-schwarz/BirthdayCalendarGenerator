package codes.thischwa.bcg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main entry point for the Birthday Calendar Generator (BCG) application.
 *
 * <p>The BcgApp class is responsible for initiating the Spring Boot application context and
 * configuring application properties. It is marked with Spring Boot annotations to enable component
 * scanning, property scanning, and logging functionality. The application's main method uses the
 * SpringApplicationBuilder to configure and start the application in a non-web environment.
 *
 * <p>Behavior: - The application starts in a non-web mode using the {@link
 * WebApplicationType#NONE}. - If an exception occurs during startup, the application logs the error
 * message and exits with status code 10.
 */
@ConfigurationPropertiesScan
@SpringBootApplication
@Slf4j
public class BcgApp {


  /**
   * Main method to launch the Birthday Calendar Generator (BCG) application.
   *
   * @param args An array of command-line arguments passed to the application on startup.
   */
  public static void main(String[] args) {
    try {
      new SpringApplicationBuilder(BcgApp.class).web(WebApplicationType.NONE).run(args);
    } catch (Exception e) {
      log.error("Unexpected exception, Spring Boot stops! Message: {}", e.getMessage());
      System.exit(10);
    }
  }
}
