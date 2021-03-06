package org.apereo.cas.services;

import org.apereo.cas.util.junit.EnabledIfContinuousIntegration;
import org.apereo.cas.util.junit.EnabledIfPortOpen;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

/**
 * Handles tests for {@link JpaServiceRegistry}
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@TestPropertySource(locations = "classpath:svcregmysql.properties")
@EnabledIfContinuousIntegration
@EnabledIfPortOpen(port = 3306)
@Tag("MySQL")
public class JpaServiceRegistryMySQLTests extends JpaServiceRegistryTests {
}
