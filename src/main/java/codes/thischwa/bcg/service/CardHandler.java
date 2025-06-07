package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Person;
import codes.thischwa.bcg.conf.DavConf;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
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
  private final Sardine sardine;

  /**
   * Constructs a new CardHandler instance to manage operations related to DAV address book
   * services.
   *
   * @param davConf The configuration object containing the credentials and URLs required for DAV
   *     integration, such as user, password, and the address book URL.
   */
  public CardHandler(DavConf davConf) {
    this.sardine = SardineFactory.begin(davConf.user(), davConf.password());
    this.davConf = davConf;
  }

  List<Person> readPeopleWithBirthday() throws IllegalArgumentException {
    List<Person> people = new ArrayList<>();
    try {
      List<DavResource> addressbookItems = sardine.list(davConf.cardUrl());
      log.info("Contacts found: {}", addressbookItems.size());

      for (DavResource contact : addressbookItems) {
        if (contact.isDirectory()) {
          continue;
        }
        log.info("Processing contact: {}", contact.getDisplayName());
        URI href = new URI(davConf.getBaseUrl() + contact.getHref().toString());
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
          people.add(
              new Person(
                  firstName,
                  lastName,
                  displayName,
                  LocalDate.parse(birthday.getValue(), birthdayFormatter)));
        } catch (IllegalArgumentException e) {
          log.warn(
              "Error while processing contact {}: {}", contact.getDisplayName(), e.getMessage());
        }
      }
      return people;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }
}
