package codes.thischwa.bcg.conf;

import codes.thischwa.bcg.AbstractIntegrationTest;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DavConfTest extends AbstractIntegrationTest {

  @Autowired
  private DavConf davConf;

  @Test
  void testDavConfPropertiesLoadedFromApplicationTestYml() {
    // Verify that the properties from application-test.yml have been correctly loaded
    assertThat(davConf.user()).isEqualTo("dev");
    assertThat(davConf.password()).isEqualTo("strong");
    assertThat(davConf.calUrl()).isEqualTo(
        "https://dav.my-domain.org/SOGo/dav/dav-user/Calendar/46-12345678-5-87654321/");
    assertThat(davConf.cardUrl()).isEqualTo(
        "https://dav.my-domain.org/SOGo/dav/dav-user/Contacts/personal/");
  }

  @Test
  void testGetBaseUrl() {
    // Verify the behavior of the getBaseUrl method
    String expectedBaseUrl = "https://dav.my-domain.org";
    assertThat(davConf.getBaseUrl()).isEqualTo(expectedBaseUrl);
  }
}