package codes.thischwa.bcg;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(resolver = TestTypeProfileResolver.class)
public abstract class AbstractTest {}
