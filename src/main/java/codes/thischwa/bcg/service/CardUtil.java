package codes.thischwa.bcg.service;

import codes.thischwa.bcg.Contact;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.property.BDay;
import net.fortuna.ical4j.vcard.property.Fn;
import net.fortuna.ical4j.vcard.property.N;

public class CardUtil {

  static final DateTimeFormatter birthdayFormatter =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  public static Contact convert(VCard card, String identifier ) {
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
    return new Contact(firstName, lastName, displayName,
        LocalDate.parse(birthday.getValue(), CardUtil.birthdayFormatter), identifier);

  }
}
