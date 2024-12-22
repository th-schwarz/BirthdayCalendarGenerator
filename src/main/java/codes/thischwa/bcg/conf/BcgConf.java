package codes.thischwa.bcg.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the BCG application. These properties are mapped from configuration
 * sources with the prefix `bcg`.
 *
 * @param product The name of the product being configured.
 * @param calendarCategory The category of the calendar.
 * @param calendarDir The directory location of the calendar files.
 * @param cron The cron expression for scheduling tasks.
 * @param runOnStart A flag indicating whether the associated task should run on application
 *     startup.
 * @param cleanConfigFile File indicator to clean the configuration.
 * @param vdirsyncerConfig The path to the vdirsyncer configuration file.
 * @param vdirsyncerStatusDir The directory where the vdirsyncer status files are stored.
 */
@ConfigurationProperties(prefix = "bcg")
public record BcgConf(
    String product,
    String calendarCategory,
    String calendarDir,
    String cron,
    boolean runOnStart,
    String cleanConfigFile,
    String vdirsyncerConfig,
    String vdirsyncerStatusDir) {}
