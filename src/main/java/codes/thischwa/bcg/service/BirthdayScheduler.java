package codes.thischwa.bcg.service;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service class that schedules the generation of the birthday calendar.
 *
 * <p>The main task performed by this service is to call the {@link
 * BirthdayCalGenerator#processBirthdayEvents()} method, which handles the generation and upload of
 * the birthday calendar.
 */
@Service
@EnableScheduling
@Profile("!test")
@Slf4j
public class BirthdayScheduler {

  private final BirthdayCalGenerator birthdayCalGenerator;

  /**
   * Constructs an instance of the BirthdayScheduler, responsible for scheduling and triggering the
   * generation of the birthday calendar using the provided BirthdayCalGenerator instance.
   *
   * @param birthdayCalGenerator the generator responsible for processing and generating the
   *     birthday calendar
   */
  public BirthdayScheduler(BirthdayCalGenerator birthdayCalGenerator) {
    this.birthdayCalGenerator = birthdayCalGenerator;
  }

  /**
   * Scheduled method that triggers the processing of the birthday calendar.
   *
   * <p>This method is executed based on the cron expression defined in the configuration property
   * `bcg.cron`.
   *
   * @throws IOException if an I/O error occurs during the processing of the birthday calendar.
   */
  @Scheduled(cron = "${bcg.cron}")
  public void process() throws IOException {
    log.info("Processing birthday calendar ...");
    birthdayCalGenerator.processBirthdayEvents();
    log.info("Processed birthday successfully.");
  }
}
