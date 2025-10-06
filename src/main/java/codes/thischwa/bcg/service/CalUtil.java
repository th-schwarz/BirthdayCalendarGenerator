package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Contact;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Uid;

import static codes.thischwa.bcg.service.CalHandler.CALENDAR_CONTENT_TYPE;

import org.springframework.lang.Nullable;

/**
 * Utility class providing methods to work with calendar events and contacts.
 */
public class CalUtil {

  /**
   * Extracts the UUID of a contact from the provided calendar event.
   * This method attempts to retrieve the value of the UID property from the event.
   * If the UID property is not available, an exception is thrown.
   *
   * @param event the VEvent object from which the contact's UUID is to be extracted
   * @return the contact's UUID as a string
   * @throws IllegalArgumentException if the UID property is not found in the event
   */
  public static String extractContactsUUIDFromEvent(VEvent event) {
    try {
      Property p = event.getProperty(Uid.UID).orElseThrow();
      return p.getValue();
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Extracts the event ID from a given event URI.
   * This method assumes the event URI contains a file name with a ".ics" extension
   * and removes the extension to retrieve the event ID.
   *
   * @param inputUrl the URL of the event from which the event ID is to be extracted
   * @return the extracted event ID as a string
   * @throws NullPointerException if the provided eventUri is null
   */
  public static String extractEventId(URL inputUrl) {
    String path = inputUrl.getPath();
    String fileName = path.substring(path.lastIndexOf('/') + 1);
    return fileName.replace(".ics", "");
  }

  /**
   * Checks if the specified event corresponds to the contact's birthday.
   *
   * @param bdEvent the VEvent object to be checked
   * @param contact the Contact object whose birthday is to be compared against the event
   * @return true if the event date matches the contact's birthday, false otherwise
   */
  public static boolean isBirthdayEquals(VEvent bdEvent, Contact contact) {
    LocalDate personBirthday = contact.birthday();
    DtStart<LocalDate> dtStart = bdEvent.getDateTimeStart();
    LocalDate eventBirthday = dtStart.getDate();
    return personBirthday.equals(eventBirthday);
  }

  public static Map<VEvent, URL> collectBirthdayEvents(Sardine sardine, String calUrl)
      throws IOException {
    Map<VEvent, URL> events = new HashMap<>();
    List<DavResource> davResources = sardine.list(calUrl);
    davResources = davResources.stream().filter(e -> !e.isDirectory()).toList();
    for (DavResource davResource : davResources) {
      if (davResource.getContentType().contains(CALENDAR_CONTENT_TYPE)) {
        URL eventUrl = new URL(NetUtil.getBaseUrl(calUrl) + davResource.getHref().getPath());
        VEvent event = convert(sardine, eventUrl);
        events.put(event, eventUrl);
      }
    }
    return events;
  }

  public static @Nullable VEvent convert(Sardine sardine, URL eventUrl)
      throws IllegalArgumentException {
    try (InputStream inputStream = sardine.get(eventUrl.toString())) {
      if (inputStream != null) {
        // Parse the iCalendar content
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = builder.build(inputStream);
        if (calendar.getComponents().size() != 1) {
          throw new IllegalArgumentException(
              "Unexpected number of calendar components: " + calendar.getComponents().size() +
                  " for URL: " + eventUrl + " (expected: 1)");
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
}
