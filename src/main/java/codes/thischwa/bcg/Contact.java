package codes.thischwa.bcg;

import java.time.LocalDate;

/**
 * Represents a contact with personal details such as name and birthday.
 *
 * @param firstName  The first name of the contact.
 * @param lastName   The last name of the contact.
 * @param displayName The display name of the contact.
 * @param birthday   The birthday of the contact.
 */
public record Contact(String firstName, String lastName, String displayName, LocalDate birthday) {
  public String getFullName() {
    return String.format("%s %s", firstName, lastName);
  }
}
