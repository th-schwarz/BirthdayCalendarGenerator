package codes.thischwa.bcg.backend;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@Slf4j
public class BaikalDockerTest extends AbstractBackendTest {

  // Baikal runs on port 80 in the container
  @Container
  public static GenericContainer<?> baikal = new GenericContainer<>("thschwarz/baikal:latest")
      .withExposedPorts(80);

  @BeforeAll
  public static void testBaikalIsRunning() throws IOException {
    String address = "http://" + baikal.getHost() + ":" + baikal.getMappedPort(80) + "/";
    log.info("Baikal URL: {}", address);

    // simple HTTP-Check
    HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
    connection.setRequestMethod("GET");
    int responseCode = connection.getResponseCode();

    assertEquals(200, responseCode, "Baikal should return HTTP 200");
  }

  @Test
  @DisplayName("BAIKAL end-to-end test")
  void completeProcess() throws Exception {
    log.info("Starting BAIKAL end-to-end test using env-configured CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
