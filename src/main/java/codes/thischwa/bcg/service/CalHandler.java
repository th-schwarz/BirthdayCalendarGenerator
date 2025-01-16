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
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Month;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;
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
   * @param conf The configuration object containing settings for the BCG system.
   * @param eventConf The configuration object for defining event-related settings.
   * @param davConf The configuration object containing WebDAV user and password details.
   */
  CalHandler(BcgConf conf, EventConf eventConf, DavConf davConf) {
    this.conf = conf;
    this.eventConf = eventConf;
    this.davConf = davConf;
    this.sardine = SardineFactory.begin(davConf.user(), davConf.password());
    this.sardine.enablePreemptiveAuthentication(davConf.getBaseUrl());
  }

  /**
   * Clears the remote calendar by removing all resources of type "text/calendar" from the specified
   * DAV server.
   *
   * @throws IOException if there is an error during communication with the DAV server while listing
   *     or deleting resources.
   */
  void clearRemoteCalendar() throws IOException {
    Set<URI> calendarEntries = getCalendarEntriesToDelete();
    if (!calendarEntries.isEmpty()) {
      log.info("{} calendar items found to be removed", calendarEntries.size());
      deleteCalendarEntries(calendarEntries);
    }
  }

  private Set<URI> getCalendarEntriesToDelete() throws IOException {
    List<DavResource> davResources = sardine.list(davConf.calUrl());
    Set<URI> calendarEntries = new HashSet<>();
    for (DavResource resource : davResources) {
      if (CALENDAR_CONTENT_TYPE.equalsIgnoreCase(resource.getContentType())) {
        log.debug(
            "Calendar found: name={}, display-name={}",
            resource.getName(),
            resource.getDisplayName());
        calendarEntries.add(resource.getHref());
      }
    }
    return calendarEntries;
  }

  private void deleteCalendarEntries(Set<URI> calendarEntries) throws IOException {
    for (URI uri : calendarEntries) {
      String path = davConf.getBaseUrl() + uri.getPath();
      sardine.delete(path);
      log.debug("Successfully deleted {}", path);
    }
  }

  void uploadEventsToCalendar(List<Person> people) {
    for (Person person : people) {
      try {
        Calendar personCal = createCalendarEvent(person);
        String eventContent = personCal.toString();

        // upload event
        String eventUrl = davConf.calUrl() + personCal.getUid().getValue() + ".ics";
        try (InputStream inputStream =
            new ByteArrayInputStream(eventContent.getBytes(StandardCharsets.UTF_8))) {
          sardine.put(eventUrl, inputStream);
          log.debug(
              "Birthday event for '{}' uploaded successful: {}", person.getFullName(), eventUrl);
        }
      } catch (Exception e) {
        log.error("Error while uploading birthday event for: {}", person.getFullName(), e);
      }
    }
  }

  private Calendar createCalendarEvent(Person person) {
    Version version = new Version();
    version.setValue(Version.VALUE_2_0);
    Calendar calendar = new Calendar();
    calendar.add(new ProdId(conf.getProdId()));
    calendar.add(version);
    calendar.add(new Method(Method.VALUE_PUBLISH));

    VEvent birthdayEvent = buildEvent(person);
    calendar.add(birthdayEvent);
    return calendar;
  }

  /**
   * Builds a VEvent instance for a specified person's birthday. The event is annually repeating.
   *
   * @param person the Person object containing the birthday and other related information
   * @return the constructed VEvent representing the person's birthday
   */
  private VEvent buildEvent(Person person) {
    String summary = eventConf.generateSummary(person);
    VEvent birthdayEvent = new VEvent(person.birthday(), summary);
    birthdayEvent.add(uidGenerator.generateUid());
    Recur<LocalDate> recur = new Recur.Builder<LocalDate>().frequency(Frequency.YEARLY).build();
    recur.getMonthList().add(Month.valueOf(person.birthday().getMonthValue()));
    recur.getMonthDayList().add(person.birthday().getDayOfMonth());
    String description = eventConf.generateDescription(person);
    birthdayEvent.add(new RRule<>(recur));
    birthdayEvent.add(new Categories(conf.calendarCategory()));
    birthdayEvent.add(new Transp(Transp.VALUE_TRANSPARENT));
    birthdayEvent.add(new Description(description));
    return birthdayEvent;
  }
}
