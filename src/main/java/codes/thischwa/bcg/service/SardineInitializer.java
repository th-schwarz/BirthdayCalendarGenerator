package codes.thischwa.bcg.service;

import codes.thischwa.bcg.conf.DavConf;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import java.io.IOException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.util.CompatibilityHints;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Slf4j
public class SardineInitializer {

  @Getter
  private final Sardine sardine;

  private final DavConf davConf;

  public SardineInitializer(DavConf davConf) {
    this.davConf = davConf;
    this.sardine = SardineFactory.begin(davConf.user(), davConf.password());
    this.sardine.enablePreemptiveAuthentication(davConf.getBaseUrl());
    CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
    CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true);
  }

  public boolean isAccessible() {
    for (int i = 0; i < davConf.maxTrails(); i++) {
      try {
        if (sardine.exists(davConf.getBaseUrl())) {
          return true;
        }
      } catch (IOException e) {
        log.warn("Error while checking access to {} (trails: {}/{}): {}", davConf.getBaseUrl(),
            i + 1, davConf.maxTrails(), e.getMessage());
      }
      try {
        Thread.sleep(davConf.delayInMinutes() * 60 * 1000L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }
}
