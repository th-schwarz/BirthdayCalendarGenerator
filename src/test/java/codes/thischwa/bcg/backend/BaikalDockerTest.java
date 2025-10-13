package codes.thischwa.bcg.backend;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@Slf4j
public class BaikalDockerTest extends AbstractBackendTest {

  private static String BASE_URL;

  private static Network network = Network.newNetwork();

  // Baikal runs on port 80 in the container
  @Container
  public static GenericContainer<?> baikal = new GenericContainer<>("thschwarz/baikal:latest")
      .withExposedPorts(80)
      .withNetwork(network)
      .waitingFor(
          Wait.forListeningPort()
              .withStartupTimeout(Duration.ofSeconds(90)));

  @BeforeAll
  public static void testBaikalIsRunning() throws IOException {
    BASE_URL = "http://" + baikal.getHost() + ":" + baikal.getMappedPort(80) + "/";
    log.info("Base URL: {}", BASE_URL);

    // simple HTTP-Check
    HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL).openConnection();
    connection.setRequestMethod("GET");
    int responseCode = connection.getResponseCode();

    assertEquals(200, responseCode, "Baikal should return HTTP 200");
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("dav.user", () -> "dev-user");
    registry.add("dav.password", () -> "test123");
    registry.add("dav.card-url", () -> BASE_URL + "dav.php/addressbooks/dev-user/default/");
    registry.add("dav.cal-url", () -> BASE_URL + "dav.php/calendars/dev-user/default/");
  }

  @Test
  @DisplayName("BAIKAL end-to-end test")
  void completeProcess() throws Exception {
    log.info("Starting BAIKAL end-to-end test using env-configured CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
