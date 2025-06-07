package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Person;
import codes.thischwa.bcg.conf.BcgConf;
import codes.thischwa.bcg.conf.DavConf;
import codes.thischwa.bcg.conf.EventConf;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Month;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import net.fortuna.ical4j.util.CompatibilityHints;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * The CalHandler class is responsible for managing calendar-related operations, including clearing
 * remote calendars and uploading calendar events for specific individuals. This class leverages
 * WebDAV for remote calendar management and utilizes configurations for event and calendar
 * properties.
 */
@Service
@Slf4j
public class CalHandler {

  private static final String CALENDAR_CONTENT_TYPE = "text/calendar";
  private final UidGenerator uidGenerator = new RandomUidGenerator();
  private final BcgConf conf;
  private final EventConf eventConf;
  private final DavConf davConf;
  private final Sardine sardine;

  /**
   * Constructor for the CalHandler class.
   *
   * @param conf      The configuration object containing settings for the BCG system.
   * @param eventConf The configuration object for defining event-related settings.
   * @param davConf   The configuration object containing WebDAV user and password details.
   */
  CalHandler(BcgConf conf, EventConf eventConf, DavConf davConf) {
    this.conf = conf;
    this.eventConf = eventConf;
    this.davConf = davConf;
    this.sardine = SardineFactory.begin(davConf.user(), davConf.password());
    this.sardine.enablePreemptiveAuthentication(davConf.getBaseUrl());
    CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
    CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true);
  }

  /**
   * Clears the remote calendar by removing all resources of type "text/calendar" from the specified
   * DAV server.
   *
   * @throws IOException if there is an error during communication with the DAV server while listing
   *                     or deleting resources.
   */
  void clearRemoteCalendar() throws IOException {
    Set<URI> calendarEntries = getRemoteEventsToDelete();
    if (!calendarEntries.isEmpty()) {
      log.info("{} calendar items found to be removed", calendarEntries.size());
      deleteRemoteEvents(calendarEntries);
    }
  }

  private Set<URI> getRemoteEventsToDelete() throws IOException {
    List<DavResource> davResources = sardine.list(davConf.calUrl());
    Set<URI> calendarEntries = new HashSet<>();
    for (DavResource resource : davResources) {
      if (CALENDAR_CONTENT_TYPE.equalsIgnoreCase(resource.getContentType())) {
        log.debug("Calendar found: name={}, display-name={}", resource.getName(),
            resource.getDisplayName());
        VEvent birthdayEvent = convert(resource);
        if (birthdayEvent != null && matchCategory(birthdayEvent)) {
          calendarEntries.add(resource.getHref());
        }
      }
    }
    return calendarEntries;
  }

  private boolean matchCategory(VEvent event) {
    return event.getCategories()
        .map(categories -> categories.getCategories().getTexts().contains(conf.calendarCategory()))
        .orElse(false);
  }

  private void deleteRemoteEvents(Set<URI> calendarEntries) throws IOException {
    for (URI uri : calendarEntries) {
      String path = davConf.getBaseUrl() + uri.getPath();
      sardine.delete(path);
      log.debug("Successfully deleted {}", path);
    }
  }

  private @Nullable VEvent convert(DavResource resource) throws IllegalArgumentException {
    String url = davConf.getBaseUrl() + resource.getHref().getPath();
    try (InputStream inputStream = sardine.get(url)) {
      if (inputStream != null) {
        // Parse the iCalendar content
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = builder.build(inputStream);
        if (calendar.getComponents().size() != 1) {
          throw new IllegalArgumentException(
              "Unexpected number of calendar components: " + calendar.getComponents().size() +
                  " for URL: " + url + " (expected: 1)");
        }

        CalendarComponent component = calendar.getComponents().get(0);
        if (component instanceof VEvent) {
          return (VEvent) component;
        }
      }
    } catch (ParserException | IOException e) {
      throw new IllegalArgumentException(e);
    }
    return null;
  }

  void uploadEventsToCalendar(List<Person> people) {
    for (Person person : people) {
      try {
        Calendar personCal = buildPersonBirthdayCalendar(person);
        String eventContent = personCal.toString();

        // upload event
        String eventUrl = davConf.calUrl() + personCal.getUid().getValue() + ".ics";
        try (InputStream inputStream = new ByteArrayInputStream(
            eventContent.getBytes(StandardCharsets.UTF_8))) {
          sardine.put(eventUrl, inputStream);
          log.debug("Birthday event for '{}' uploaded successful: {}", person.getFullName(),
              eventUrl);
        }
      } catch (Exception e) {
        log.error("Error while uploading birthday event for: {}", person.getFullName(), e);
      }
    }
  }

  private Calendar buildPersonBirthdayCalendar(Person person) {
    Version version = new Version();
    version.setValue(Version.VALUE_2_0);
    Calendar calendar = new Calendar();
    calendar.add(new ProdId(conf.getProdId()));
    calendar.add(version);
    calendar.add(new Method(Method.VALUE_PUBLISH));

    VEvent birthdayEvent = buildBirthdayEvent(person);
    calendar.add(birthdayEvent);
    return calendar;
  }

  /**
   * Builds a VEvent instance for a specified person's birthday. The event is annually repeated.
   *
   * @param person the Person object containing the birthday and other related information
   * @return the constructed VEvent representing the person's birthday
   */
  private VEvent buildBirthdayEvent(Person person) {
    String summary = eventConf.generateSummary(person);
    String description = eventConf.generateDescription(person);
    VEvent birthdayEvent = new VEvent(person.birthday(), summary);
    birthdayEvent.add(uidGenerator.generateUid());

    // build and add the repetition rule
    Recur<LocalDate> recur = new Recur.Builder<LocalDate>().frequency(Frequency.YEARLY).build();
    recur.getMonthList().add(Month.valueOf(person.birthday().getMonthValue()));
    recur.getMonthDayList().add(person.birthday().getDayOfMonth());
    birthdayEvent.add(new RRule<>(recur));

    if (eventConf.getAlarmDuration() != null) {
      // build and add an alarm
      VAlarm alarm = new VAlarm();
      alarm.add(new Trigger(eventConf.getAlarmDuration()));
      alarm.add(new Action(Action.VALUE_DISPLAY));
      alarm.add(new Description(description));
      alarm.add(new Summary(summary));
      birthdayEvent.add(alarm);
    }

    // add other properties
    birthdayEvent.add(new Categories(conf.calendarCategory()));
    birthdayEvent.add(new Transp(Transp.VALUE_TRANSPARENT));
    birthdayEvent.add(new Description(description));

    return birthdayEvent;
  }
}