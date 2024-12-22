package codes.thischwa.bcg;

import java.time.LocalDate;

/**
 * A record that represents a person with basic personal details.
 *
 * @param firstName The person's first name.
 * @param lastName The person's last name.
 * @param displayName A display name for the person, which can be a nickname
 * or any alternative representation of their name.
 * @param birthday The person's date of birth.
 */
public record Person(String firstName, String lastName, String displayName, LocalDate birthday) {}
