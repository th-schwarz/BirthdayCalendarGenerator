package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Contact;
import codes.thischwa.bcg.conf.DavConf;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;
import net.fortuna.ical4j.vcard.property.BDay;
import net.fortuna.ical4j.vcard.property.Fn;
import net.fortuna.ical4j.vcard.property.N;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

/**
 * Handles operations related to the DAV address book, including fetching and processing contact
 * information such as people with birthdays.
 */
@Slf4j
@Component
public class CardHandler {

  private static final DateTimeFormatter birthdayFormatter =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  private final DavConf davConf;
  private final SardineInitializer sardineInitializer;

  /**
   * Constructs a new CardHandler instance to manage operations related to DAV address book
   * services.
   *
   * @param davConf            The configuration object containing the credentials and URLs required
   *                           for DAV integration, such as user, password, and the address book
   *                           URL.
   * @param sardineInitializer The initializer for {@link Sardine}.
   */
  public CardHandler(DavConf davConf, SardineInitializer sardineInitializer) {
    this.sardineInitializer = sardineInitializer;
    this.davConf = davConf;
  }

  List<Contact> readPeopleWithBirthday() throws IllegalArgumentException {
    if (!sardineInitializer.isAccessible()) {
      log.error("Access to {} timed out after {} trails.", davConf.getBaseUrl(), davConf.maxTrails());
      throw new IllegalArgumentException("Access to " + davConf.getBaseUrl() + " timed out.");
    }
    Sardine sardine = sardineInitializer.getSardine();
    List<Contact> contacts = new ArrayList<>();
    try {
      List<DavResource> addressbookItems = sardine.list(davConf.cardUrl());
      log.info("Contacts found: {}", addressbookItems.size());

      for (DavResource davResource : addressbookItems) {
        if (davResource.isDirectory()) {
          continue;
        }
        log.info("Processing contact: {}",
            (davResource.getDisplayName() == null) ? "display name n/a" :
                davResource.getDisplayName());
        URI href = new URI(davConf.getBaseUrl() + davResource.getHref().toString());
        try (InputStream vCardStream = sardine.get(href.toString())) {
          byte[] vcfContent = IOUtils.toByteArray(vCardStream);
          VCardBuilder cardBuilder = new VCardBuilder(new ByteArrayInputStream(vcfContent));
          VCard card = cardBuilder.build();
          PropertyList propertyList = card.getEntities().get(0).getPropertyList();
          Optional<BDay<LocalDate>> optBday = propertyList.getFirst(BDay.class.getSimpleName());
          BDay<LocalDate> birthday =
              optBday.orElseThrow(() -> new IllegalArgumentException("Missing birthday"));

          Optional<Fn> optFn = propertyList.getFirst(Fn.class.getSimpleName());
          Optional<N> optName = propertyList.getFirst(N.class.getSimpleName());

          String displayName = optFn.isPresent() ? optFn.get().getValue() : "";
          N fullName = optName.orElseThrow(() -> new IllegalArgumentException("Missing name"));

          String firstName = fullName.getGivenName();
          String lastName = fullName.getFamilyName();
          contacts.add(new Contact(firstName, lastName, displayName,
              LocalDate.parse(birthday.getValue(), birthdayFormatter)));
        } catch (IllegalArgumentException e) {
          log.warn("Error while processing contact {}: {}", davResource.getDisplayName(),
              e.getMessage());
        }
      }
      return contacts;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }
}
