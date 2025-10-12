package codes.thischwa.bcg;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * Test configuration for the BCG application.
 * This class provides the Spring Boot configuration specifically for test environments.
 */
@ConfigurationPropertiesScan
@PropertySource("classpath:/application-test.yml")
@SpringBootApplication
@Profile({"test", "backend-test"})
public class TestBcgApp {
    // No need for main method or other beans - this class just serves as a configuration anchor
}
