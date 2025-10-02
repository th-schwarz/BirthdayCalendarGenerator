package codes.thischwa.bcg;

import codes.thischwa.bcg.it.AbstractIntegrationTest;

import org.springframework.test.context.ActiveProfilesResolver;

/**
 * A custom implementation of {@link ActiveProfilesResolver} that determines the
 * active Spring profiles for a given test class.
 *
 * This resolver checks if the provided test class is a subtype of
 * {@link AbstractIntegrationTest}. If it is, the active profile is set to
 * {@code "it-test"}. Otherwise, the default active profile is set to
 * {@code "test"}.
 *
 * This class is useful for dynamically configuring the testing profile
 * based on the inheritance of the test class.
 */
public class TestTypeProfileResolver implements ActiveProfilesResolver {
    
    @Override
    public String[] resolve(Class<?> testClass) {
        if (AbstractIntegrationTest.class.isAssignableFrom(testClass)) {
            return new String[] {"it-test"};
        } else {
            return new String[] {"test"};
        }
    }
}
