package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Person;
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
   * Processes the birthday calendar by performing the following operations:
   *
   * <ol>
   *   <li>Clears the remote calendar through the `calHandler`.
   *   <li>Reads a list of people with birthdays using the `cardHandler`.
   *   <li>Uploads the generated birthday events to a calendar through the `calHandler`.
   * </ol>
   *
   * <p>The actual approach is 'brute-force'.
   *
   * @throws IOException if an I/O error occurs during any of the processing steps.
   */
  public void processBirthdayEvents() throws IOException {
    log.info("Processing clear remote calendar ...");
    calHandler.clearRemoteCalendar();
    log.info("Processed clear remote calendar successfully.");

    log.info("Processing birthday events ...");
    List<Person> people = cardHandler.readPeopleWithBirthday();
    calHandler.uploadEventsToCalendar(people);
    log.info("Processed birthday events successfully.");
  }
}
