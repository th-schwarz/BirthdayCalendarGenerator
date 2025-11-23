package codes.thischwa.bcg.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
public class SogoComposeTest extends AbstractBackendTest {

  private static String BASE_URL;
  private static final int SERVICE_PORT = 80;
  private static final String SERVICE_NAME = "nginx";

  @Container
  public static ComposeContainer sogo = new ComposeContainer(
      new File("src/docker/sogo/docker-compose.yml"))
      .withLocalCompose(true)
      .withPull(true)
      .withExposedService(
          SERVICE_NAME,
          SERVICE_PORT,
          Wait.forHttp("/SOGo/")
              .forPort(SERVICE_PORT)
              .forStatusCodeMatching(status -> status >= HttpStatus.SC_OK && status < 500)
              .withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT_SEC))
      );

  @BeforeAll
  public static void testIsRunning() throws IOException, InterruptedException {
    String host = sogo.getServiceHost(SERVICE_NAME, SERVICE_PORT);
    Integer port = sogo.getServicePort(SERVICE_NAME, SERVICE_PORT);
    BASE_URL = "http://" + host + ":" + port + "/";
    log.info("Base URL: {}", BASE_URL);

    // Give SOGo additional time to fully initialize
    log.info("Waiting additional 10 seconds for SOGo to fully initialize...");
    Thread.sleep(10000);

    // simple HTTP-Check
    HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + "SOGo/").openConnection();
    connection.setRequestMethod("GET");
    connection.setInstanceFollowRedirects(false);
    int responseCode = connection.getResponseCode();

    assertTrue(responseCode >= 200 && responseCode < 500,
        "SOGo should be reachable over HTTP (2xx/3xx/4xx). Actual: " + responseCode);
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    // These credentials are aligned with src/docker/sogo/config/init-users.sql
    registry.add("dav.user", () -> "dev-user");
    registry.add("dav.password", () -> "test123");
    registry.add("dav.card-url", () -> BASE_URL + "SOGo/dav/dev-user/Contacts/personal/");
    registry.add("dav.cal-url", () -> BASE_URL + "SOGo/dav/dev-user/Calendar/personal/");
  }

  @Test
  @DisplayName("SOGO end-to-end test (docker-compose)")
  void completeProcess() throws Exception {
    log.info("Starting SOGO end-to-end test using docker-compose CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
