package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Person;
import codes.thischwa.bcg.conf.BcgConf;
import codes.thischwa.bcg.conf.DavConf;
import codes.thischwa.bcg.conf.EventConf;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Month;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

/**
 * The CalHandler class provides methods for managing and handling calendar-related operations. It
 * supports generating iCalendar (.ics) files, clearing remote calendars, and managing local
 * calendar directories.
 */
@Service
@Slf4j
public class CalHandler {

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
  public CalHandler(BcgConf conf, EventConf eventConf, DavConf davConf) {
    this.conf = conf;
    this.eventConf = eventConf;
    this.davConf = davConf;
    this.sardine = SardineFactory.begin(davConf.user(), davConf.password());
  }

  /**
   * Clears the remote calendar by removing all resources of type "text/calendar" from the specified
   * DAV server.
   *
   * @throws IOException if there is an error during communication with the DAV server while listing
   *     or deleting resources.
   */
  public void clearRemoteCalendar() throws IOException {
    List<DavResource> resources = sardine.list(davConf.calUrl());
    Set<URI> toRemove = new HashSet<>();
    for (DavResource resource : resources) {
      if (resource.getContentType().equalsIgnoreCase("text/calendar")) {
        log.debug(
            "Calender found: name={}, display-name={}",
            resource.getName(),
            resource.getDisplayName());
        toRemove.add(resource.getHref());
      }
    }
    if (!toRemove.isEmpty()) {
      log.info("{} items found to be removed", toRemove.size());
      for (URI uri : toRemove) {
        String path = davConf.getBaseUrl() + uri.getPath();
        sardine.delete(path);
        log.debug("Successfully deleted {}", path);
      }
    }
  }

  /**
   * Generates iCalendar (.ics) files for a list of people. This method generates birthday events
   * for each person and writes the corresponding .ics files to the specified directory.
   *
   * @param people A list of Person objects for whom the iCalendar files will be generated.
   * @throws IOException If an I/O error occurs during the creation of directories or files.
   * @throws IllegalArgumentException If the specified calendar directory cannot be found.
   */
  public void writeBirthdayEventsToFiles(List<Person> people) throws IOException {
    Path calDir = Paths.get(conf.calendarDir());
    if (!Files.exists(calDir)) {
      // VdirSyncerConfigurationService#checkConfig wasn't run
      throw new IllegalArgumentException("Calendar directory not found: " + calDir);
    }
    Path calCollectionPath = Paths.get(conf.calendarDir(), davConf.getCalendarPath());
    if (!Files.exists(calCollectionPath)) {
      FileUtils.forceMkdir(calCollectionPath.toFile());
      log.info("Cal collection not found and created.");
    }

    String prodId = String.format("-//%s//iCal4j 1.0//EN", conf.product());
    for (Person p : people) {
      // Generate the calendar
      Calendar calendar = new Calendar().withProdId(prodId).withDefaults().getFluentTarget();
      VEvent birthdayEvent = buildEvent(p);
      calendar.add(birthdayEvent);
      calendar.validate();

      // Write the calendar to a file
      Path icsPath =
          Path.of(calCollectionPath.toString(), birthdayEvent.getUid().get().getValue() + ".ics");
      writeFile(calendar, icsPath);
      log.info("ICS file generated successfully for {}: {}", p, icsPath.toAbsolutePath());
    }
  }

  /**
   * Clears the contents of the calendar directory by deleting a specific subdirectory
   * and recreating it.
   *
   * @throws IOException if an I/O error occurs during the deletion or creation of the directory
   * @throws IllegalArgumentException if the base calendar directory does not exist
   */
  public void clearCalendarDir() throws IOException {
    Path baseCalPath = Path.of(conf.calendarDir());
    if (!Files.exists(baseCalPath)) {
      throw new IllegalArgumentException("Base cal-dir not found: " + baseCalPath);
    }
    Optional<Path> optIcalPath = fetchFirstSubDir(baseCalPath);
    if (optIcalPath.isPresent()) {
      Path icalPath = optIcalPath.get().toAbsolutePath();
      log.info("{} already exists and will be deleted.", icalPath);
      FileUtils.deleteDirectory(icalPath.toFile());
      Files.createDirectory(icalPath);
    }
  }

  VEvent buildEvent(Person p) {
    String summary = eventConf.generateSummary(p);
    String description = eventConf.generateDescription(p);
    VEvent birthdayEvent = new VEvent(p.birthday(), summary);
    birthdayEvent.add(uidGenerator.generateUid());
    Recur<LocalDate> recur = new Recur.Builder<LocalDate>().frequency(Frequency.YEARLY).build();
    recur.getMonthList().add(Month.valueOf(p.birthday().getMonthValue()));
    recur.getMonthDayList().add(p.birthday().getDayOfMonth());
    birthdayEvent.add(new RRule<>(recur));
    birthdayEvent.add(new Categories(conf.calendarCategory()));
    birthdayEvent.add(new Transp(Transp.VALUE_TRANSPARENT));
    birthdayEvent.add(new Description(description));
    return birthdayEvent;
  }

  private void writeFile(Calendar calendar, Path filePath) throws IOException {
    try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile())) {
      CalendarOutputter calendarOutputter = new CalendarOutputter();
      calendarOutputter.output(calendar, fileOutputStream);
    }
  }

  private Optional<Path> fetchFirstSubDir(Path dir) throws IOException {
    try (Stream<Path> paths = Files.list(dir)) {
      return paths.filter(Files::isDirectory).findFirst();
    }
  }
}
