package codes.thischwa.bcg.service;

import codes.thischwa.bcg.conf.BcgConf;
import codes.thischwa.bcg.conf.DavConf;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

/**
 * Service for configuring and managing VdirSyncer.
 *
 * <p>This service ensures that the necessary configurations and directories for VdirSyncer are in
 * place and handles creating the configuration file if it doesn't exist.
 */
@Slf4j
@Service
public class VdirSyncerConfigurationService {

  private final DavConf davConf;

  private final BcgConf bcgConf;

  private final VdirSyncerCaller vdirSyncerCaller;

  public VdirSyncerConfigurationService(
          DavConf davConf, BcgConf bcgConf, VdirSyncerCaller vdirSyncerCaller) {
    this.davConf = davConf;
    this.bcgConf = bcgConf;
    this.vdirSyncerCaller = vdirSyncerCaller;
  }

  public void checkConfig() throws IOException {
    Path cleanPath = Paths.get(bcgConf.cleanConfigFile());
    if (Files.exists(cleanPath)) {
      log.info("Clean config file [{}] exists, configuration will be deleted!.", cleanPath);
      cleanConfig();
      Files.delete(cleanPath);
    }
    try {
      vdirSyncerCaller.check();
    } catch (Exception e) {
      log.error("'vdirsyncer' isn't installed, please install it first.");
      throw new RuntimeException(e);
    }
    Path calPath = Paths.get(bcgConf.calendarDir());
    if (!Files.exists(calPath)) {
      FileUtils.forceMkdir(calPath.toFile());
      log.info("Cal folder not found and created.");
    }
    Path vdirSyncerConfigPath = Paths.get(bcgConf.vdirsyncerConfig());
    if (Files.exists(vdirSyncerConfigPath)) {
      log.info("Config file for 'vdirsyncer' already exists, skip processing.");
      return;
    }
    String config = generateConfig();
    try {
      Files.writeString(
          Paths.get(bcgConf.vdirsyncerConfig()), config, StandardOpenOption.CREATE_NEW);
      log.info("Config file for 'vdirsyncer' created successful: {}", bcgConf.vdirsyncerConfig());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void cleanConfig() {
    Path path = Paths.get(bcgConf.vdirsyncerConfig());
    boolean done = FileUtils.deleteQuietly(path.toFile());
    if (!done) {
      log.warn("Could not delete: " + path);
    }
    path = Paths.get(bcgConf.vdirsyncerStatusDir());
    done = FileUtils.deleteQuietly(path.toFile());
    if (!done) {
      log.warn("Could not delete: " + path);
    }
    path = Paths.get(bcgConf.calendarDir());
    done = FileUtils.deleteQuietly(path.toFile());
    if (!done) {
      log.warn("Could not delete: " + path);
    }
  }

  String generateConfig() {
    String config;
    try (InputStream is = getClass().getResourceAsStream("/vdirsyncer.config.template")) {
      assert is != null;
      byte[] bytes = is.readAllBytes();
      config = new String(bytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    config =
        String.format(
            config,
            bcgConf.vdirsyncerStatusDir(),
            bcgConf.calendarDir(),
            davConf.calUrl(),
            davConf.user(),
            davConf.password());
    return config;
  }
}
