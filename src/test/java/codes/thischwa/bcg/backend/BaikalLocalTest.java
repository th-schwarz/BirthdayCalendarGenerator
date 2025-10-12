package codes.thischwa.bcg.backend;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Slf4j
@Disabled("Only for local testing")
class BaikalLocalTest extends AbstractBackendTest {

  private static final String DAV_USER = System.getenv("BAIKAL_DAV_USER");
  private static final String DAV_PASS = System.getenv("BAIKAL_DAV_PASS");
  private static final String DAV_CARD_URL = System.getenv("BAIKAL_DAV_CARD_URL");
  private static final String DAV_CAL_URL = System.getenv("BAIKAL_DAV_CAL_URL");
  
  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("dav.user", () -> DAV_USER);
    registry.add("dav.password", () -> DAV_PASS);
    registry.add("dav.card-url", () -> DAV_CARD_URL);
    registry.add("dav.cal-url", () -> DAV_CAL_URL);
  }

  @BeforeEach
  void checkEnvironment() {
    boolean envSet =
      DAV_USER != null && DAV_PASS != null && DAV_CARD_URL != null && DAV_CAL_URL != null;
    if (!envSet) {
      log.warn("BAIKAL environment variables not set, skipping integration test");
    }
    assumeTrue(envSet,
      "BAIKAL environment variables must be set for integration test");
  }

  @Test
  void completeProcess() throws Exception {
    log.info("Starting BAIKAL end-to-end test using env-configured CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
