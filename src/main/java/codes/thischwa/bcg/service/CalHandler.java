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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Month;
import net.fortuna.ical4j.model.Property;
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
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import net.fortuna.ical4j.util.CompatibilityHints;

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

  private Summary buildSummary(Person person) {
    String summary = eventConf.generateSummary(person);
    return new Summary(summary);
  }

  /**
   * Builds a VEvent instance for a specified person's birthday. The event is annually repeated.
   *
   * @param person the Person object containing the birthday and other related information
   * @return the constructed VEvent representing the person's birthday
   */
  private VEvent buildBirthdayEvent(Person person) {
    Summary summary = buildSummary(person);
    String description = eventConf.generateDescription(person);
    VEvent birthdayEvent = new VEvent(person.birthday(), summary.getValue());
    birthdayEvent.add(new Uid(generatePersonUUID(person)));

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
      alarm.add(summary);
      birthdayEvent.add(alarm);
    }

    // add other properties
    birthdayEvent.add(new Categories(conf.calendarCategory()));
    birthdayEvent.add(new Transp(Transp.VALUE_TRANSPARENT));
    birthdayEvent.add(new Description(description));

    return birthdayEvent;
  }

  void syncEventsWithBirthdayChanges(List<Person> currentPeople) throws IOException {
    List<Person> changedOrMissingPeople = findChangedOrMissingBirthdays(currentPeople);
    if (changedOrMissingPeople.isEmpty()) {
      log.info("No birthday events to update found. Sync stopped.");
      return;
    }
    log.info("Found {} changed or missing birthday events.", changedOrMissingPeople.size());

    Map<String, URI> existingEventUris = new HashMap<>();
    List<DavResource> davResources = sardine.list(davConf.calUrl());
    for (DavResource resource : davResources) {
      if (CALENDAR_CONTENT_TYPE.equalsIgnoreCase(resource.getContentType())) {
        String eventId = extractEventId(resource.getHref());
        existingEventUris.put(eventId, resource.getHref());
      }
    }
    log.debug("Found {} existing birthday events", existingEventUris.size());

    for (Person person : changedOrMissingPeople) {
      Calendar personCal = buildPersonBirthdayCalendar(person);
      uploadSingleEvent(personCal, person);
      log.info("Added or updated event for: {}", person.getFullName());

      existingEventUris.remove(generatePersonUUID(person));
    }
    for (URI uri : existingEventUris.values()) {
      sardine.delete(davConf.getBaseUrl() + uri.getPath());
      log.info("Deleted outdated event: {}", uri.getPath());
    }
  }

  private List<Person> findChangedOrMissingBirthdays(List<Person> currentPeople)
      throws IOException {
    // Schritt 1: Lade alle bestehenden Geburtstagsereignisse aus dem Kalender
    Map<String, VEvent> existingEvents = new HashMap<>();
    List<DavResource> davResources = sardine.list(davConf.calUrl());
    for (DavResource resource : davResources) {
      if (CALENDAR_CONTENT_TYPE.equalsIgnoreCase(resource.getContentType())) {
        VEvent event = convert(resource);
        if (event != null && matchCategory(event)) {
          String uuid = extractPersonUUIDFromEvent(event); // UUID für die Person extrahieren
          existingEvents.put(uuid, event); // Map: UUID -> VEvent
        }
      }
    }

    // Schritt 2: Vergleiche die aktuellen Personen mit den bestehenden Ereignissen
    List<Person> changedOrMissingPeople = new ArrayList<>();
    for (Person person : currentPeople) {
      String uuid = generatePersonUUID(person); // UUID für Vergleich generieren
      VEvent existingEvent = existingEvents.get(uuid);

      if (existingEvent == null || !isEventUpToDate(existingEvent, person)) {
        // Person hat kein entsprechendes Event oder Event ist nicht mehr aktuell
        changedOrMissingPeople.add(person);
      }
    }

    return changedOrMissingPeople;
  }

  private String extractPersonUUIDFromEvent(VEvent event)  {
    try {
      Property p = event.getProperty(Uid.UID).orElseThrow();
      return p.getValue();
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private String extractEventId(URI eventUri) {
    // Extrahiere die UUID aus der URI des Kalendereintrags
    String path = eventUri.getPath();
    String[] segments = path.split("/");
    String fileName = segments[segments.length - 1]; // Name der Datei extrahieren
    return fileName.replace(".ics", ""); // ".ics" entfernen, um die ID zu erhalten
  }

  private String generatePersonUUID(Person person) {
    String uniqueId = person.getFullName() + "_" + person.birthday();
    return uniqueId.replaceAll("[^a-zA-Z0-9]", "_");
  }

  private boolean isEventUpToDate(VEvent event, Person person) {
    return buildSummary(person).equals(event.getSummary().get()) &&
        person.birthday().equals(event.getDateTimeStart().get().getDate());
  }

  private void uploadSingleEvent(Calendar calendar, Person person) throws IOException {
    String eventContent = calendar.toString();
    String eventUrl = davConf.calUrl() + generatePersonUUID(person) + ".ics";
    try (InputStream inputStream = new ByteArrayInputStream(
        eventContent.getBytes(StandardCharsets.UTF_8))) {
      sardine.put(eventUrl, inputStream);
      log.debug("Uploaded birthday event for '{}': {}", person.getFullName(), eventUrl);
    }
  }

  private VEvent getDavResource(URI uri) {
    try (InputStream inputStream = sardine.get(davConf.getBaseUrl() + uri.getPath())) {
      CalendarBuilder builder = new CalendarBuilder();
      Calendar calendar = builder.build(inputStream);
      if (!calendar.getComponents().isEmpty() &&
          calendar.getComponents().get(0) instanceof VEvent) {
        return (VEvent) calendar.getComponents().get(0);
      } else {
        throw new IllegalArgumentException("No valid VEvent found in calendar resource.");
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Error parsing DAV resource", e);
    }
  }
}