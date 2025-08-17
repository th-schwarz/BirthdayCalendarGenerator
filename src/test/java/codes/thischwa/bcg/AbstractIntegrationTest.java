package codes.thischwa.bcg;

import codes.thischwa.bcg.conf.BcgConf;
import codes.thischwa.bcg.conf.DavConf;
import codes.thischwa.bcg.service.BirthdayCalGenerator;
import codes.thischwa.bcg.service.CalUtil;
import codes.thischwa.bcg.service.SardineInitializer;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Content;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Uid;
import static codes.thischwa.bcg.service.CalHandler.CALENDAR_CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(resolver = TestTypeProfileResolver.class)
@Slf4j
public abstract class AbstractIntegrationTest {

  @Autowired
  protected BirthdayCalGenerator generator;

  @Autowired
  protected DavConf davConf;

  @Autowired
  protected BcgConf bcgConf;

  @Autowired
  protected SardineInitializer sardineInitializer;

  private final Contact janeWithBirthDay = new Contact("Jane", "Doe", "J. Doe",
      LocalDate.of(1990, 5, 12));
  private final Contact johnWithBDay = new Contact("John", "Smith", "J. Smith",
      LocalDate.of(1985, 11, 3));
  private final Contact richard = new Contact("Richard", "Smith", "R. Smith", null);

  void syncAndVerify() throws Exception {
    Sardine sardine = sardineInitializer.getSardine();
    // 4) Run sync and verify 2 events
    log.info("Step 4: Running BirthdayCalGenerator.processBirthdayEvents and verifying 2 events");
    generator.processBirthdayEvents();
    List<VEvent> eventsAfterFirstSync = listBirthdayEvents(sardine);
    assertEquals(2, eventsAfterFirstSync.size(), "Expected exactly 2 birthday events");

    // Keep track of UIDs
    List<String> uids = eventsAfterFirstSync.stream()
        .map(e -> e.getProperty(Uid.UID).map(Content::getValue).orElse(""))
        .toList();

    // 5) Change the birthday of one contact and verify
    log.info("Step 5: Changing Jane Doe's birthday and re-synchronizing");
    LocalDate janeNewBday = janeWithBirthDay.birthday().plusDays(1);
    Contact janeUpdated = new Contact(janeWithBirthDay.firstName(), janeWithBirthDay.lastName(),
        janeWithBirthDay.displayName(), janeNewBday);
    putVCard(davConf.cardUrl() + "c1.vcf", buildVCard(janeUpdated));
    generator.processBirthdayEvents();
    List<VEvent> eventsAfterChange = listBirthdayEvents(sardine);
    assertEquals(2, eventsAfterChange.size(), "Expected exactly 2 birthday events");

    // Check updated BDay
    Optional<VEvent> janeNew = eventsAfterChange.stream().filter(e -> e.getProperty(Uid.UID)
        .map(p -> p.getValue()).orElse("").startsWith(janeWithBirthDay.firstName() + "_" + janeWithBirthDay.lastName())).findFirst();
    assertTrue(janeNew.isPresent(), "Jane's updated event not found after sync");

    Object dateObj = janeNew.get().getDateTimeStart().getDate();
    assertInstanceOf(LocalDate.class, dateObj,
        "Unexpected DtStart date type: " + dateObj.getClass());
    LocalDate dtStartNew = (LocalDate) dateObj;
    assertEquals(janeNewBday, dtStartNew, "Jane's event date should reflect the changed birthday");

    // 6) Delete a contact with a birthday and verify event removal
    log.info("Step 6: Deleting John Smith contact and re-synchronizing");
    sardine.delete(davConf.cardUrl() + "c2.vcf");
    generator.processBirthdayEvents();

    List<VEvent> eventsAfterDeletion = listBirthdayEvents(sardine);

    // John Smith UID prefix
    String johnUidPrefix = ("John_Smith_" + johnWithBDay.birthday()).replaceAll("[^a-zA-Z0-9]", "_");
    boolean johnEventStillExists = eventsAfterDeletion.stream().anyMatch(e -> e.getProperty(Uid.UID)
        .map(p -> p.getValue()).orElse("").startsWith(johnUidPrefix));
    assertFalse(johnEventStillExists, "John Smith's birthday event should be deleted after contact removal");

    log.info("All steps completed successfully.");
  }

  void prepareRemote() throws IOException {
    log.info("Step 1: Clearing remote address book at {}", davConf.cardUrl());
    clearRemoteCard();

    log.info("Step 2: Clearing remote calendar at {}", davConf.calUrl());
    clearRemoteCalendar();

    log.info("Step 3: Adding contacts (2 with birthdays, 1 without)");
    putVCard(davConf.cardUrl() + "c1.vcf", buildVCard(janeWithBirthDay));
    putVCard(davConf.cardUrl() + "c2.vcf", buildVCard(johnWithBDay));
    putVCard(davConf.cardUrl() + "c3.vcf", buildVCard(richard));
  }

  private String buildVCard(Contact contact) {
    List<String> lines = new ArrayList<>();
    lines.add("BEGIN:VCARD");
    lines.add("VERSION:3.0");
    if (contact.birthday() != null) {
      lines.add("UID:" + CalUtil.createContactIdentifier(contact));
    }
    lines.add("N:" + contact.lastName() + ";" + contact.firstName() + ";;;");
    lines.add("FN:" + contact.displayName());
    LocalDate bday = contact.birthday();
    if (bday != null) {
      String val = String.format("%04d%02d%02d", bday.getYear(), bday.getMonthValue(), bday.getDayOfMonth());
      lines.add("BDAY:" + val);
    }
    lines.add("END:VCARD");
    return String.join("\n", lines);
  }

  private  void putVCard(String url, String content) throws IOException {
    Sardine sardine = sardineInitializer.getSardine();
    log.info("Uploading vCard: {}", url);
    try (InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
      sardine.put(url, is);
    }
  }

  /**
   * Clears all non-directory resources from the remote card DAV server. This method retrieves the
   * list of resources from the server specified by the card URL and removes each resource that is
   * not a directory.
   * <p>
   * Each deleted resource's path is logged once the deletion is successful.
   *
   * @throws IOException if an I/O error occurs while listing or deleting resources on the DAV
   *                     server.
   */
  void clearRemoteCard() throws IOException {
    Sardine sardine = sardineInitializer.getSardine();
    List<DavResource> davResources = sardine.list(davConf.cardUrl());
    for (DavResource resource : davResources) {
      if (!resource.isDirectory()) {
        String path = davConf.getBaseUrl() + resource.getHref().getPath();
        sardine.delete(path);
        log.debug("Successfully deleted {}", path);
      }
    }
  }

  /**
   * Clears the remote calendar by removing all resources of the type "text / calendar" from the
   * specified DAV server.
   *
   * @throws IOException if there is an error during communication with the DAV server while listing
   *                     or deleting resources.
   */
  void clearRemoteCalendar() throws IOException {
    Set<URI> calendarEntries = getCalendarEntriesToDelete();
    if (!calendarEntries.isEmpty()) {
      log.info("{} calendar items found to be removed", calendarEntries.size());
      deleteCalendarEntries(calendarEntries);
    }
  }

  private Set<URI> getCalendarEntriesToDelete() throws IOException {
    Sardine sardine = sardineInitializer.getSardine();
    List<DavResource> davResources = sardine.list(davConf.calUrl());
    Set<URI> calendarEntries = new HashSet<>();
    for (DavResource resource : davResources) {
      if (CALENDAR_CONTENT_TYPE.equalsIgnoreCase(resource.getContentType())) {
        log.debug("Calendar found: name={}, display-name={}", resource.getName(),
            resource.getDisplayName());
        calendarEntries.add(resource.getHref());
      }
    }
    return calendarEntries;
  }

  private void deleteCalendarEntries(Set<URI> calendarEntries) throws IOException {
    Sardine sardine = sardineInitializer.getSardine();
    for (URI uri : calendarEntries) {
      String path = davConf.getBaseUrl() + uri.getPath();
      sardine.delete(path);
      log.debug("Successfully deleted {}", path);
    }
  }

  private List<VEvent> listBirthdayEvents(Sardine sardine) throws Exception {
    List<VEvent> events = new ArrayList<>();
    List<DavResource> items = sardine.list(davConf.calUrl());
    for (DavResource res : items) {
      if (res.isDirectory()) continue;
      String url = davConf.getBaseUrl() + res.getHref().getPath();
      try (InputStream is = sardine.get(url)) {
        CalendarBuilder builder = new CalendarBuilder();
        Calendar cal = builder.build(Objects.requireNonNull(is));
        cal.getComponents().forEach(c -> {
          if (c instanceof VEvent ev) {
            boolean isBirthdayCategory = ev.getProperty(Categories.CATEGORIES)
                .map(p -> p.getValue()).map(bcgConf.calendarCategory()::equals).orElse(false);
            if (isBirthdayCategory) {
              events.add(ev);
            }
          }
        });
      }
    }
    log.info("Found {} birthday events.", events.size());
    return events;
  }
}
