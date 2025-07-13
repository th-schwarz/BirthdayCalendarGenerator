package codes.thischwa.bcg.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Configuration properties for DAV integration. These properties are mapped from configuration
 * sources with the prefix `dav`.
 *
 * @param user           The username for authentication.
 * @param password       The password for authentication.
 * @param calUrl         The URL for accessing calendar services.
 * @param cardUrl        The URL for accessing address book services.
 * @param delayInMinutes The delay in minutes for scheduled tasks or updates.
 * @param maxTrails The maximum number of trials for a specific operation.
 */
@ConfigurationProperties(prefix = "dav")
public record DavConf(
    String user, String password, String calUrl, String cardUrl, Integer delayInMinutes,
    Integer maxTrails) {

  /**
   * Retrieves the base URL derived from the `cardUrl` property. It removes any path, query,
   * or fragment components from the URL.
   *
   * @return The base URL as a String.
   */
  public String getBaseUrl() {
    return UriComponentsBuilder.fromUriString(cardUrl)
        .replacePath(null)
        .replaceQuery(null)
        .fragment(null)
        .build()
        .toUriString();
  }
}
