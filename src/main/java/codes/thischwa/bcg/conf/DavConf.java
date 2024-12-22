package codes.thischwa.bcg.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Configuration properties for DAV integration. These properties are mapped from configuration
 * sources with the prefix `dav`.
 *
 * @param user The username for authentication.
 * @param password The password for authentication.
 * @param davPath Optional path for DAV-specific configurations.
 * @param calUrl The URL for accessing calendar services.
 * @param cardUrl The URL for accessing address book services.
 */
@ConfigurationProperties(prefix = "dav")
public record DavConf(
    String user, String password, @Nullable String davPath, String calUrl, String cardUrl) {

  public String getBaseUrl() {
    return UriComponentsBuilder.fromUriString(cardUrl)
        .replacePath(null)
        .replaceQuery(null)
        .fragment(null)
        .build()
        .toUriString();
  }

  /**
   * Extracts and returns the trailing path segment of the calendar URL.
   * If the URL ends with a forward slash, the trailing slash is removed first.
   * It then identifies the last segment of the URL after the final forward slash.
   *
   * @return The last segment of the calendar URL, representing the calendar path.
   */
  public String getCalendarPath() {
    String path = calUrl.endsWith("/") ? calUrl.substring(0, calUrl.length() - 1) : calUrl;
    int idx = path.lastIndexOf('/');
    if (idx > 0) {
      path = path.substring(idx + 1);
    }
    return path;
  }
}
