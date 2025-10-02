package codes.thischwa.bcg.it;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class SogoTest extends AbstractIntegrationTest {

  private static final String DAV_USER = System.getenv("SOGO_DAV_USER");
  private static final String DAV_PASS = System.getenv("SOGO_DAV_PASS");
  private static final String DAV_CARD_URL = System.getenv("SOGO_DAV_CARD_URL");
  private static final String DAV_CAL_URL = System.getenv("SOGO_DAV_CAL_URL");

  @BeforeEach
  void checkEnvironment() {
    boolean envSet =
      DAV_USER != null && DAV_PASS != null && DAV_CARD_URL != null && DAV_CAL_URL != null;
    if (!envSet) {
      log.warn("SOGo environment variables not set, skipping integration test");
    }
    assumeTrue(envSet,
      "SOGo environment variables must be set for integration test");
  }

  @Test
  void completeProcess() throws Exception {
    log.info("Starting SOGo end-to-end test using env-configured CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
