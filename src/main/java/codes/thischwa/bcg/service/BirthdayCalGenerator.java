package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Contact;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service responsible for generating and uploading birthday calendars. */
@Service
@Slf4j
public class BirthdayCalGenerator {

  private final CalHandler calHandler;
  private final CardHandler cardHandler;

  /**
   * Constructs an instance of BirthdayCalGenerator, which is responsible for managing and
   * generating birthday calendars through various handlers and components.
   *
   * @param calHandler  the handler responsible for calendar-related operations such as clearing and
   *                    generating calendar files
   * @param cardHandler the handler responsible for managing and reading card data (e.g., people
   *                    with birthdays)
   */
  public BirthdayCalGenerator(CalHandler calHandler, CardHandler cardHandler) {
    this.calHandler = calHandler;
    this.cardHandler = cardHandler;
  }

  /**
   * Processes and synchronizes birthday events.
   *
   * <p>This method retrieves a list of people with birthdays from the card handler and syncs these
   * details with the calendar using the calendar handler. It ensures that all birthday events
   * in the calendar reflect any changes in the underlying data source, such as additions,
   * updates, or deletions of birthdays.
   *
   * @throws IOException if an I/O error occurs during synchronization operations.
   */
  public void processBirthdayEvents() throws IOException {
    log.info("Syncing birthday events ...");
    List<Contact> people = cardHandler.readPeopleWithBirthday();
    calHandler.syncEventsWithBirthdayChanges(people);
    log.info("Synced birthday events successfully.");
  }
}
