package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Contact;
import java.net.URI;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Uid;

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
   * @param eventUri the URI of the event from which the event ID is to be extracted
   * @return the extracted event ID as a string
   * @throws NullPointerException if the provided eventUri is null
   */
  public static String extractEventId(URI eventUri) {
    String path = eventUri.getPath();
    String[] segments = path.split("/");
    String fileName = segments[segments.length - 1];
    return fileName.replace(".ics", "");
  }

  /**
   * Creates a unique identifier for a contact by concatenating the contact's full name
   * and birthday, replacing any non-alphanumeric characters with underscores.
   *
   * @param contact the Contact object for which the identifier is to be created
   * @return a sanitized string representing the unique identifier for the contact
   */
  public static String createContactIdentifier(Contact contact) {
    if (contact.birthday() == null) {
      throw new IllegalArgumentException("Contact birthday must not be null in this context.");
    }
    String uniqueId = contact.getFullName() + "_" + contact.birthday();
    return uniqueId.replaceAll("[^a-zA-Z0-9]", "_");
  }

  /**
   * Checks if the specified event corresponds to the contact's birthday.
   *
   * @param bdEvent   the VEvent object to be checked
   * @param contact the Contact object whose birthday is to be compared against the event
   * @return true if the event date matches the contact's birthday, false otherwise
   */
  public static boolean isBirthdayEquals(VEvent bdEvent, Contact contact) {
    LocalDate personBirthday = contact.birthday();
    DtStart<LocalDate> dtStart = bdEvent.getDateTimeStart();
    LocalDate eventBirthday = dtStart.getDate();
    return personBirthday.equals(eventBirthday);
  }
}
