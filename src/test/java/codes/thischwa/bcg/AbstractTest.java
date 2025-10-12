package codes.thischwa.bcg;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestBcgApp.class)
@ActiveProfiles("test")
public abstract class AbstractTest {}
