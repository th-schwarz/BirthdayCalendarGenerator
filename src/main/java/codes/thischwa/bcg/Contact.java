package codes.thischwa.bcg;

import java.time.LocalDate;

import org.springframework.lang.Nullable;

/**
 * Represents a contact with personal details such as name and birthday.
 *
 * @param firstName   The first name of the contact.
 * @param lastName    The last name of the contact.
 * @param displayName The display name of the contact.
 * @param birthday    The birthday of the contact.
 * @param identifier  The unique identifier of the contact.
 */
public record Contact(String firstName, String lastName, String displayName,
                      @Nullable LocalDate birthday, @Nullable String identifier) {

  public Contact(String firstName, String lastName, String displayName, @Nullable LocalDate birthday) {
    this(firstName, lastName, displayName, birthday, null);
  }

  public String getFullName() {
    return String.format("%s %s", firstName, lastName);
  }
}
