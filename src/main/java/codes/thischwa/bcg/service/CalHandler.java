package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Contact;
import codes.thischwa.bcg.conf.BcgConf;
import codes.thischwa.bcg.conf.DavConf;
import codes.thischwa.bcg.conf.EventConf;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
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
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Month;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.transform.recurrence.Frequency;

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

  public static final String CALENDAR_CONTENT_TYPE = "text/calendar";
  private final BcgConf conf;
  private final EventConf eventConf;
  private final DavConf davConf;
  private final SardineInitializer sardineInitializer;

  /**
   * Constructor for the CalHandler class.
   *
   * @param conf               The configuration object containing settings for the BCG system.
   * @param eventConf          The configuration object for defining event-related settings.
   * @param davConf            The configuration object containing WebDAV user and password
   *                           details.
   * @param sardineInitializer The initializer for {@link Sardine}.
   */
  CalHandler(BcgConf conf, EventConf eventConf, DavConf davConf,
      SardineInitializer sardineInitializer) {
    this.conf = conf;
    this.eventConf = eventConf;
    this.davConf = davConf;
    this.sardineInitializer = sardineInitializer;
  }

  void syncEventsWithBirthdayChanges(List<Contact> contacts) throws IOException {
    if (!sardineInitializer.canAccessBaseUrl()) {
      log.error("Access to {} timed out after {} trails.", davConf.getBaseUrl(), davConf.maxRetries());
      throw new IllegalArgumentException("Access to " + davConf.getBaseUrl() + " timed out.");
    }
    Sardine sardine = sardineInitializer.getSardine();
    log.info("Syncing birthday events of {} contacts.", contacts.size());

    Map<String, VEvent> existingEvents = new HashMap<>();
    Map<String, Contact> existingContacts = new HashMap<>();
    List<DavResource> davResources = sardine.list(davConf.calUrl());
    Map<String, URI> existingEventUris = new HashMap<>();

    // collecting all events matching the desired category
    for (DavResource resource : davResources) {
      if (CALENDAR_CONTENT_TYPE.equalsIgnoreCase(resource.getContentType())) {
        VEvent event = convert(resource, sardine);
        if (event != null && matchCategory(event)) {
          String uuid = CalUtil.extractContactsUUIDFromEvent(event);
          existingEvents.put(uuid, event);
          String eventId = CalUtil.extractEventId(resource.getHref());
          existingEventUris.put(eventId, resource.getHref());
        }
      }
    }

    // delete birthday events from contacts whose doesn't exist
    contacts.forEach(person -> existingContacts.put(CalUtil.createContactIdentifier(person), person));
    existingEvents.keySet().forEach((eventUuid) -> {
      if (!existingContacts.containsKey(eventUuid)) {
        URI eventUri = existingEventUris.get(eventUuid);
        try {
          sardine.delete(davConf.getBaseUrl() + eventUri.getPath());
          log.debug("Deleted outdated event: {}", eventUri.getPath());
        } catch (IOException e) {
          log.error("Failed to delete outdated event: {}", eventUri.getPath(), e);
        }
      }
    });

    List<Contact> changedPeople = new ArrayList<>();
    // collect contacts whose birthday has changed
    for (Contact contact : contacts) {
      String uuid = CalUtil.createContactIdentifier(contact);
      VEvent existingEvent = existingEvents.get(uuid);

      if (existingEvent == null || !CalUtil.isBirthdayEquals(existingEvent, contact)) {
        changedPeople.add(contact);
        log.debug("New or updated event for: {}", contact.getFullName());
      }
    }
    if (changedPeople.isEmpty()) {
      log.info("No birthday events to update found. Sync stopped.");
      return;
    }

    // process changed or missing birthday event
    for (Contact contact : changedPeople) {
      Calendar personCal = buildBirthdayCalendar(contact);
      String uuid = CalUtil.createContactIdentifier(contact);
      if (existingEventUris.containsKey(uuid)) {
        URI eventUri = existingEventUris.get(uuid);
        sardine.delete(davConf.getBaseUrl() + eventUri.getPath());
        log.debug("Deleted outdated event before add: {}", eventUri.getPath());
      }
      uploadSingleEvent(sardine, personCal, contact);
      log.info("Added or updated event for: {}", contact.getFullName());
    }
  }

  private boolean matchCategory(VEvent event) {
    return event.getCategories().stream().anyMatch(
        categories -> categories.getCategories().getTexts().contains(conf.calendarCategory()));
  }

  private @Nullable VEvent convert(DavResource resource, Sardine sardine) throws IllegalArgumentException {
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

  private Calendar buildBirthdayCalendar(Contact contact) {
    Version version = new Version();
    version.setValue(Version.VALUE_2_0);
    Calendar calendar = new Calendar();
    calendar.add(new ProdId(conf.getProdId()));
    calendar.add(version);
    calendar.add(new CalScale(CalScale.VALUE_GREGORIAN)); //

    VEvent birthdayEvent = buildBirthdayEvent(contact);
    calendar.add(birthdayEvent);
    return calendar;
  }

  private Summary buildSummary(Contact contact) {
    String summary = eventConf.generateSummary(contact);
    return new Summary(summary);
  }

  /**
   * Builds a VEvent instance for a specified person's birthday. The event is annually repeated.
   *
   * @param contact the Person object containing the birthday and other related information
   * @return the constructed VEvent representing the person's birthday
   */
  private VEvent buildBirthdayEvent(Contact contact) {
    Summary summary = buildSummary(contact);
    String description = eventConf.generateDescription(contact);
    VEvent birthdayEvent = new VEvent(contact.birthday(), summary.getValue());
    birthdayEvent.add(new Uid(CalUtil.createContactIdentifier(contact)));

    // build and add the repetition rule
    Recur<LocalDate> recur = new Recur.Builder<LocalDate>().frequency(Frequency.YEARLY).build();
    recur.getMonthList().add(Month.valueOf(contact.birthday().getMonthValue()));
    recur.getMonthDayList().add(contact.birthday().getDayOfMonth());
    birthdayEvent.add(new RRule<>(recur));

    if (eventConf.getAlarmDuration() != null) {
      // build and add an alarm
      VAlarm alarm = new VAlarm();

      // create trigger with VALUE=DURATION explicitly
      Trigger trigger = new Trigger(eventConf.getAlarmDuration());
      trigger.add(Value.DURATION);
      alarm.add(trigger);
      alarm.add(new Action(Action.VALUE_DISPLAY));
      alarm.add(new Description(description));
      alarm.add(summary);
      birthdayEvent.add(alarm);
    }

    // add other properties
    birthdayEvent.add(new Categories(conf.calendarCategory()));
    birthdayEvent.add(new Transp(Transp.VALUE_TRANSPARENT));
    birthdayEvent.add(new Description(description));
    birthdayEvent.add(new Status(Status.VALUE_CONFIRMED));
    return birthdayEvent;
  }

  private void uploadSingleEvent(Sardine sardine, Calendar calendar, Contact contact) throws IOException {
    String eventContent = calendar.toString();
    String eventUrl = davConf.calUrl() + CalUtil.createContactIdentifier(contact) + ".ics";
    try (InputStream inputStream = new ByteArrayInputStream(
        eventContent.getBytes(StandardCharsets.UTF_8))) {
      sardine.put(eventUrl, inputStream);
      log.debug("Uploaded birthday event for '{}': {}", contact.getFullName(), eventUrl);
    }
  }
}