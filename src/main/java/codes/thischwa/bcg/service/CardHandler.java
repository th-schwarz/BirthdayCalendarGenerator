package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Contact;
import codes.thischwa.bcg.conf.DavConf;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
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

  List<Contact> readContactsWithBirthday() throws IllegalArgumentException {
    if (!sardineInitializer.canAccessBaseUrl()) {
      log.error("Access to {} timed out after {} trails.", davConf.getBaseUrl(),
          davConf.maxRetries());
      throw new IllegalArgumentException("Access to " + davConf.getBaseUrl() + " timed out.");
    }
    Sardine sardine = sardineInitializer.getSardine();
    List<Contact> contacts = new ArrayList<>();
    try {
      List<DavResource> vcardResources = sardine.list(davConf.cardUrl())
          .stream()
          .filter(item -> !item.isDirectory())
          .toList();
      log.info("Contacts found: {}", vcardResources.size());

      for (DavResource davResource : vcardResources) {
        log.info("Processing contact: {}",
            (davResource.getDisplayName() == null ||
                davResource.getDisplayName().isEmpty()) ?
                davResource.toString() :
                davResource.getDisplayName());
        URI href = new URI(davConf.getBaseUrl() + davResource.getHref().toString());
        try (InputStream vCardStream = sardine.get(href.toString())) {
          byte[] vcfContent = IOUtils.toByteArray(vCardStream);
          VCardBuilder cardBuilder =
              new VCardBuilder(new ByteArrayInputStream(vcfContent));
          VCard card = cardBuilder.build();
          String identifier = CalUtil.extractEventId(href.toURL());
          Contact contact = CardUtil.convert(card, identifier);
          contacts.add(contact);
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
