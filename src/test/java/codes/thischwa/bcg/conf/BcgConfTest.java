package codes.thischwa.bcg.conf;

import codes.thischwa.bcg.AbstractIntegrationTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BcgConfTest extends AbstractIntegrationTest {

    @Autowired
    private BcgConf bcgConf;

    @Test
    void testProperties() {
        assertEquals("BirthdayCalendarGenerator", bcgConf.product());
        assertEquals("Birthday", bcgConf.calendarCategory());
        assertEquals("0 30 4 * * SUN", bcgConf.cron());
        assertFalse(bcgConf.runOnStart());
    }

    @Test
    void testGetProdId() {
        String expectedProdId = "-//BirthdayCalendarGenerator//iCal4j 1.0//EN";
        assertEquals(expectedProdId, bcgConf.getProdId());
    }

}