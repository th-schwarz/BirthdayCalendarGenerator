package codes.thischwa.bcg.conf;

import codes.thischwa.bcg.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DavConfTest extends AbstractIntegrationTest {

  @Autowired
  private DavConf davConf;

  @Test
  void testDavConfPropertiesLoadedFromApplicationTestYml() {
    assertEquals("dev", davConf.user());
    assertEquals("strong", davConf.password());
    assertEquals(
        "https://dav.my-domain.org/SOGo/dav/dav-user/Calendar/46-12345678-5-87654321/",
        davConf.calUrl()
    );
    assertEquals(
        "https://dav.my-domain.org/SOGo/dav/dav-user/Contacts/personal/",
        davConf.cardUrl()
    );
    assertEquals(1, davConf.delayInMinutes());
    assertEquals(5, davConf.maxTrails());
  }

  @Test
  void testGetBaseUrl() {
    String expectedBaseUrl = "https://dav.my-domain.org";
    assertEquals(expectedBaseUrl, davConf.getBaseUrl());
  }
}