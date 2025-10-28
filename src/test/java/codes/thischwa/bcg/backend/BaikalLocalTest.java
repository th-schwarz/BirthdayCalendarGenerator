package codes.thischwa.bcg.backend;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Slf4j
@Disabled("Only for local testing")
class BaikalLocalTest extends AbstractBackendTest {

  private static final String BASE_URL = "http://localhost:8080/";

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("dav.user", () -> "dev-user");
    registry.add("dav.password", () -> "test123");
    registry.add("dav.card-url", () -> BASE_URL + "dav.php/addressbooks/dev-user/default/");
    registry.add("dav.cal-url", () -> BASE_URL + "dav.php/calendars/dev-user/default/");
  }

  @Test
  void completeProcess() throws Exception {
    log.info("Starting BAIKAL end-to-end test using env-configured CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
