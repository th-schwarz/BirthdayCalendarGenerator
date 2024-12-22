package codes.thischwa.bcg.conf;

import codes.thischwa.bcg.Person;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for event-related settings. These properties are mapped from
 * configuration sources with the prefix `event`.
 *
 * @param summary The template used to generate a summary for an event.
 * @param description The template used to generate a description for an event.
 * @param dateFormat The date format pattern used for formatting dates within the templates.
 */
@ConfigurationProperties(prefix = "event")
public record EventConf(String summary, String description, String dateFormat) {

  public String generateSummary(Person person) {
    return replace(summary, person);
  }

  public String generateDescription(Person person) {
    return replace(description, person);
  }

  private String replace(String template, Person person) {
    DateTimeFormatter df = DateTimeFormatter.ofPattern(dateFormat);
    return template
        .replace("~first-name~", person.firstName())
        .replace("~last-name~", person.lastName())
        .replace("~display-name~", person.displayName())
        .replace("~birthday~", df.format(person.birthday()));
  }
}
