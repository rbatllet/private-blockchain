package com.rbatllet.blockchain.testutil;

import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for blockchain tests with automatic genesis key management.
 *
 * <p><strong>Usage:</strong> Extend this class instead of using raw JUnit tests:
 * <pre>{@code
 * public class MyBlockchainTest extends BaseBlockchainTest {
 *     // Your tests here - genesis keys are automatically available
 * }
 * }</pre>
 *
 * <p><strong>What this provides:</strong>
 * <ul>
 *   <li>Automatic genesis-admin key generation if not present</li>
 *   <li>Thread-safe key loading (prevents race conditions in parallel tests)</li>
 *   <li>Cached keys (avoids redundant I/O)</li>
 *   <li>Clear logging of key status</li>
 * </ul>
 *
 * <p><strong>⚠️ TEST-ONLY:</strong> This automatic key generation is ONLY safe in tests.
 * Production code should use {@code ./tools/generate_genesis_keys.zsh}
 *
 * @see GenesisKeyManager
 * @since 1.0.6
 */
public abstract class BaseBlockchainTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseBlockchainTest.class);

    /**
     * Ensure genesis-admin keys exist before ANY test runs.
     *
     * <p>This method runs once per test class (not per test method).
     * It's thread-safe and idempotent.
     */
    @BeforeAll
    static void ensureGenesisKeysExist() {
        logger.debug("🔍 Checking genesis-admin keys availability...");
        GenesisKeyManager.ensureGenesisKeysExist();
        logger.debug("✅ Genesis-admin keys ready for tests");
    }
}
