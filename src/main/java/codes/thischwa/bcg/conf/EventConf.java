package codes.thischwa.bcg.conf;

import codes.thischwa.bcg.Person;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

/**
 * Configuration properties for event-related settings. These properties are mapped from
 * configuration sources with the prefix `event`.
 */
@Slf4j
@ConfigurationProperties(prefix = "event")
public class EventConf {

  private String summary;
  private String description;
  private @Getter String dateFormat;

  @Nullable
  private @Getter Duration alarmDuration;

  public EventConf(String summary, String description, String dateFormat, String alarm) {
    this.summary = summary;
    this.description = description;
    this.dateFormat = dateFormat;

    if (!alarm.isBlank()) {
      Pattern pattern = Pattern.compile("(\\d+)([dh])");
      Matcher matcher = pattern.matcher(alarm);
      if (matcher.matches()) {
        int number = Integer.parseInt(matcher.group(1)) * -1;
        String unit = matcher.group(2);
        if ("d".equals(unit)) {
          alarmDuration = Duration.ofDays(number);
        } else if ("h".equals(unit)) {
          alarmDuration = Duration.ofHours(number);
        }
      } else {
        log.warn("Invalid alarm configuration found. Expected format: <number>[dh], actual: {}", alarm);
      }

    } else {
      log.debug("No alarm configuration found.");
    }
  }

  /**
   * Generates a summary for the specified person by replacing placeholders in the summary template
   * with the person's details.
   *
   * @param person The person whose details will be used to populate the summary template.
   * @return A string containing the generated summary with placeholders replaced by the person's
   * details.
   */
  public String generateSummary(Person person) {
    return replace(summary, person);
  }

  /**
   * Generates a description for the specified person by replacing placeholders in the description
   * template with the person's details.
   *
   * @param person The person whose details will be used to populate the description template.
   * @return A string containing the generated description with placeholders replaced by the
   * person's details.
   */
  public String generateDescription(Person person) {
    return replace(description, person);
  }

  private String replace(String template, Person person) {
    DateTimeFormatter df = DateTimeFormatter.ofPattern(dateFormat);
    return template.replace("~first-name~", person.firstName())
        .replace("~last-name~", person.lastName()).replace("~display-name~", person.displayName())
        .replace("~birthday~", df.format(person.birthday()));
  }
}