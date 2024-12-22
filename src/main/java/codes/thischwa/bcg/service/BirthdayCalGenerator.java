package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Person;
import codes.thischwa.bcg.conf.BcgConf;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

/** Service responsible for generating and synchronizing birthday calendars. */
@Service
@Slf4j
public class BirthdayCalGenerator {

  private final BcgConf conf;
  private final CalHandler calHandler;
  private final CardHandler cardHandler;
  private final VdirSyncerCaller vdirSyncerCaller;

  /**
   * Constructs an instance of BirthdayCalGenerator, which is responsible for managing and
   * generating birthday calendars through various handlers and components.
   *
   * @param conf the configuration object specifying settings for the birthday calendar generation
   *     process
   * @param calHandler the handler responsible for calendar-related operations such as clearing and
   *     generating calendar files
   * @param cardHandler the handler responsible for managing and reading card data (e.g., people
   *     with birthdays)
   * @param vdirSyncerCaller the utility responsible for synchronizing and discovering calendar data
   */
  public BirthdayCalGenerator(
      BcgConf conf,
      CalHandler calHandler,
      CardHandler cardHandler,
      VdirSyncerCaller vdirSyncerCaller) {
    this.conf = conf;
    this.calHandler = calHandler;
    this.cardHandler = cardHandler;
    this.vdirSyncerCaller = vdirSyncerCaller;
  }

  /**
   * Processes the birthday calendar by performing a series of operations. The actual approach is
   * 'brute-force'.
   *
   * <p>This method performs the following steps sequentially:
   *
   * <ol>
   *   <li>Cleans the directory specified by the `vdirsyncerStatusDir` configuration.
   *   <li>Logs the start and completion of the calendar clearing process.
   *   <li>Clears the calendar using the `calHandler`.
   *   <li>Clears the calendar directory using the `cardHandler`.
   *   <li>Calls the `vdirSyncerProcessor` to discover calendar settings.
   *   <li>Logs the start and completion of the iCalendar file generation process.
   *   <li>Generates iCalendar files using the `cardHandler`.
   *   <li>Logs the start and completion of the synchronization process.
   *   <li>Calls the `vdirSyncerProcessor` to synchronize the calendar.
   * </ol>
   *
   * <p>Currently this is the "brute-force" way.
   *
   * @throws IOException if an I/O error occurs during directory cleaning, calendar clearing, or
   *     file generation.
   */
  public void processBirthdayCal() throws IOException {
    Path statusDir = Paths.get(conf.vdirsyncerStatusDir());
    if (Files.exists(statusDir)) {
      FileUtils.cleanDirectory(statusDir.toFile());
    }
    log.info("Processing clean-up ...");
    calHandler.clearRemoteCalendar();
    calHandler.clearCalendarDir();
    log.info("Processed clean-up successfully.");

    log.info("Processing #generateICalFiles ...");
    List<Person> people = cardHandler.readPeopleWithBirthday();
    calHandler.writeBirthdayEventsToFiles(people);
    log.info("Processed #generateICalFiles successfully.");

    vdirSyncerCaller.callDiscover();

    log.info("Processing #callSync ...");
    vdirSyncerCaller.callSync();
    log.info("Processed #callSync successfully.");
  }
}
