package codes.thischwa.bcg;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Slf4j
class SogoTest extends AbstractIntegrationTest {

  private static final String DAV_USER = System.getenv("SOGO_DAV_USER");
  private static final String DAV_PASS = System.getenv("SOGO_DAV_PASS");
  private static final String DAV_CARD_URL = System.getenv("SOGO_DAV_CARD_URL");
  private static final String DAV_CAL_URL = System.getenv("SOGO_DAV_CAL_URL");

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry r) {
    // Only register properties if we're running integration tests
    String[] profiles = new TestTypeProfileResolver().resolve(SogoTest.class);
    boolean isIntegrationTest = java.util.Arrays.asList(profiles).contains("it-test");
    
    if (!isIntegrationTest) {
      log.info("Skipping SOGo property registration - not running as integration test");
      return;
    }

    r.add("dav.user", () -> DAV_USER);
    r.add("dav.password", () -> DAV_PASS);
    r.add("dav.card-url", () -> DAV_CARD_URL);
    r.add("dav.cal-url", () -> DAV_CAL_URL);
    r.add("dav.max-retries", () -> 2);
    r.add("dav.retry-delay-in-minutes", () -> 1);
  }

  @BeforeEach
  void checkEnvironment() {
    assumeTrue(DAV_USER != null && DAV_PASS != null && DAV_CARD_URL != null && DAV_CAL_URL != null,
        "SOGo environment variables must be set for integration test");
  }

  @Test
  void completeProcess() throws Exception {
    log.info("Starting SOGo end-to-end test using env-configured CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
