package com.example.minimarketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring context loads without errors.
 * Full unit + integration tests come in the next sprint.
 */
@SpringBootTest
@ActiveProfiles("test")
class MinimarketplaceApplicationTests {

    @Test
    void contextLoads() {
        // If the context starts successfully this test passes.
    }
}
