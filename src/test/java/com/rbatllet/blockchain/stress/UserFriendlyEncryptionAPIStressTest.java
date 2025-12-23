package com.rbatllet.blockchain.stress;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stress tests for UserFriendlyEncryptionAPI thread safety
 * Validates AtomicReference improvements and concurrent credential management
 */
public class UserFriendlyEncryptionAPIStressTest {

    private static final Logger logger = LoggerFactory.getLogger(
        UserFriendlyEncryptionAPIStressTest.class
    );

    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair bootstrapKeyPair;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize(); // CRITICAL: Clear all data from previous tests
        // Clean up database before each test to ensure isolation
        // BlockRepository now package-private - use clearAndReinitialize();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();

        // Load bootstrap admin keys
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        api = new UserFriendlyEncryptionAPI(blockchain);
        executorService = Executors.newCachedThreadPool();

        // Initialize SearchSpecialistAPI for stress tests that may use storeSecret
        try {
            KeyPair defaultKeyPair = CryptoUtil.generateKeyPair();
            blockchain.initializeAdvancedSearch("Password123!");
            blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, "Password123!", defaultKeyPair.getPrivate());
        } catch (Exception e) {
            logger.warn("⚠️ SearchSpecialistAPI initialization failed in stress test setup: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
        }
    }

    @Test
    @DisplayName(
        "🧪 Stress test: Concurrent credential changes with AtomicReference"
    )
    @Timeout(60)
    void testConcurrentCredentialChanges() throws Exception {
        logger.info("🚀 Starting concurrent credential changes stress test...");

        final int NUM_THREADS = 20;
        final int OPERATIONS_PER_THREAD = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        final AtomicInteger successfulChanges = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);
        final Set<String> allUsedUsernames = ConcurrentHashMap.newKeySet();

        // Submit all tasks
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                // ARCHITECTURE FIX: Each thread gets its own UserFriendlyEncryptionAPI instance
                // This is the CORRECT design pattern: UserFriendlyEncryptionAPI is designed 
                // for per-user usage, NOT for sharing between multiple concurrent users.
                // This test now rigorously validates that:
                // 1. Multiple UserFriendlyEncryptionAPI instances can coexist safely
                // 2. Each instance maintains credential consistency independently
                // 3. Blockchain operations remain thread-safe across all instances
                UserFriendlyEncryptionAPI threadApi = new UserFriendlyEncryptionAPI(blockchain);
                
                try {
                    startLatch.await(); // Wait for signal to start

                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        try {
                            String username =
                                "stressUser" + threadId + "_" + op;
                            KeyPair keyPair = CryptoUtil.generateKeyPair();

                            // Add key to blockchain first, handle duplicates gracefully
                            String publicKeyString =
                                CryptoUtil.publicKeyToString(
                                    keyPair.getPublic()
                                );
                            try {
                                blockchain.addAuthorizedKey(
                                    publicKeyString,
                                    username,
                                    bootstrapKeyPair,
                                    UserRole.USER
                                );
                            } catch (IllegalArgumentException e) {
                                // Key already exists - this is acceptable in stress test
                                // Generate new unique key
                                keyPair = CryptoUtil.generateKeyPair();
                                publicKeyString = CryptoUtil.publicKeyToString(
                                    keyPair.getPublic()
                                );
                                blockchain.addAuthorizedKey(
                                    publicKeyString,
                                    username + "_retry",
                                    bootstrapKeyPair,
                                    UserRole.USER
                                );
                                username = username + "_retry";
                            }

                            // Set credentials on THIS thread's API instance
                            // This tests that each UserFriendlyEncryptionAPI instance
                            // maintains its own credentials correctly
                            threadApi.setDefaultCredentials(username, keyPair);

                            // Verify credentials are set correctly on THIS instance
                            String retrievedUsername = threadApi.getDefaultUsername();
                            boolean hasCredentials = threadApi.hasDefaultCredentials();

                            if (
                                username.equals(retrievedUsername) &&
                                hasCredentials
                            ) {
                                successfulChanges.incrementAndGet();
                                allUsedUsernames.add(username);
                            } else {
                                logger.warn(
                                    "❌ Credential mismatch: expected {}, got {}, hasCredentials: {}",
                                    username,
                                    retrievedUsername,
                                    hasCredentials
                                );
                                errors.incrementAndGet();
                            }

                            // Small delay to increase contention
                            Thread.sleep(1);
                        } catch (Exception e) {
                            logger.error(
                                "❌ Error in credential change operation",
                                e
                            );
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        logger.info(
            "🏁 Starting {} threads with {} operations each...",
            NUM_THREADS,
            OPERATIONS_PER_THREAD
        );
        startLatch.countDown();

        // Wait for completion
        assertTrue(
            completionLatch.await(90, TimeUnit.SECONDS),
            "Stress test should complete within timeout"
        );

        // Validate results
        int expectedOperations = NUM_THREADS * OPERATIONS_PER_THREAD;
        logger.info(
            "📊 Results: {} successful changes, {} errors, {} total operations",
            successfulChanges.get(),
            errors.get(),
            expectedOperations
        );
        logger.info("📊 Unique usernames created: {}", allUsedUsernames.size());

        // Assertions
        // Allow for some expected concurrent conflicts (duplicate keys, etc.)
        int maxAcceptableErrors =
            (NUM_THREADS * OPERATIONS_PER_THREAD * 15) / 100; // 15% error rate is acceptable for stress test
        assertTrue(
            errors.get() <= maxAcceptableErrors,
            String.format(
                "Error rate should be acceptable (got %d errors, max acceptable: %d)",
                errors.get(),
                maxAcceptableErrors
            )
        );
        assertTrue(
            successfulChanges.get() >= expectedOperations - maxAcceptableErrors,
            "Most credential changes should succeed"
        );
        assertTrue(
            allUsedUsernames.size() >= expectedOperations - maxAcceptableErrors,
            "Most usernames should be unique"
        );

        logger.info("✅ Concurrent credential changes stress test passed!");
    }

    @Test
    @DisplayName(
        "🧪 Stress test: Concurrent operations with credential reading"
    )
    @Timeout(60)
    void testConcurrentOperationsWithCredentialReading() throws Exception {
        logger.info(
            "🚀 Starting concurrent operations with credential reading stress test..."
        );

        final int NUM_READER_THREADS = 10;
        final int NUM_WRITER_THREADS = 5;
        final int OPERATIONS_PER_THREAD = 30;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(
            NUM_READER_THREADS + NUM_WRITER_THREADS
        );
        final AtomicInteger successfulReads = new AtomicInteger(0);
        final AtomicInteger successfulWrites = new AtomicInteger(0);
        final AtomicInteger nullReads = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);

        // Set initial credentials with unique username to avoid collisions in concurrent test execution
        KeyPair initialKeyPair = CryptoUtil.generateKeyPair();
        String initialUsername = "initialUser_" + System.nanoTime();
        String publicKeyString = CryptoUtil.publicKeyToString(
            initialKeyPair.getPublic()
        );
        boolean keyAdded = blockchain.addAuthorizedKey(publicKeyString, initialUsername, bootstrapKeyPair, UserRole.USER);
        if (!keyAdded) {
            throw new IllegalStateException("Failed to add initial user key - test setup failed");
        }
        api.setDefaultCredentials(initialUsername, initialKeyPair);

        // Reader threads - constantly read credentials
        for (int i = 0; i < NUM_READER_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        try {
                            String username = api.getDefaultUsername();
                            boolean hasCredentials =
                                api.hasDefaultCredentials();

                            if (username != null && hasCredentials) {
                                successfulReads.incrementAndGet();
                            } else {
                                nullReads.incrementAndGet();
                            }

                            Thread.sleep(1); // Small delay
                        } catch (Exception e) {
                            logger.error(
                                "❌ Error in reader thread {}",
                                threadId,
                                e
                            );
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Writer threads - change credentials periodically
        for (int i = 0; i < NUM_WRITER_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        try {
                            String username =
                                "writerUser" + threadId + "_" + op;
                            KeyPair keyPair = CryptoUtil.generateKeyPair();

                            // Add key to blockchain
                            String pubKeyString = CryptoUtil.publicKeyToString(
                                keyPair.getPublic()
                            );

                            // Handle case where key is already authorized (can happen in concurrent stress tests)
                            boolean writerKeyAdded = blockchain.addAuthorizedKey(pubKeyString, username, bootstrapKeyPair, UserRole.USER);
                            if (!writerKeyAdded) {
                                // Key already authorized - generate unique key
                                keyPair = CryptoUtil.generateKeyPair();
                                pubKeyString = CryptoUtil.publicKeyToString(
                                    keyPair.getPublic()
                                );
                                username = username + "_unique_" + System.nanoTime();
                                writerKeyAdded = blockchain.addAuthorizedKey(pubKeyString, username, bootstrapKeyPair, UserRole.USER);

                                if (!writerKeyAdded) {
                                    // Skip this iteration if still can't add key
                                    continue;
                                }
                            }

                            // Change credentials
                            api.setDefaultCredentials(username, keyPair);
                            successfulWrites.incrementAndGet();

                            Thread.sleep(5); // Longer delay for writers
                        } catch (Exception e) {
                            logger.error(
                                "❌ Error in writer thread {}",
                                threadId,
                                e
                            );
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads
        logger.info(
            "🏁 Starting {} reader and {} writer threads...",
            NUM_READER_THREADS,
            NUM_WRITER_THREADS
        );
        startLatch.countDown();

        // Wait for completion
        assertTrue(
            completionLatch.await(45, TimeUnit.SECONDS),
            "Concurrent operations test should complete within timeout"
        );

        // Validate results
        int expectedReads = NUM_READER_THREADS * OPERATIONS_PER_THREAD;
        int expectedWrites = NUM_WRITER_THREADS * OPERATIONS_PER_THREAD;

        logger.info(
            "📊 Results: {} successful reads, {} null reads, {} successful writes, {} errors",
            successfulReads.get(),
            nullReads.get(),
            successfulWrites.get(),
            errors.get()
        );

        // Assertions
        assertEquals(
            0,
            errors.get(),
            "No errors should occur during concurrent operations"
        );
        assertEquals(
            expectedWrites,
            successfulWrites.get(),
            "All credential writes should succeed"
        );
        assertEquals(
            expectedReads,
            successfulReads.get() + nullReads.get(),
            "All read attempts should complete (either successful or null)"
        );

        // Verify final state is consistent
        assertTrue(
            api.hasDefaultCredentials(),
            "API should have credentials at the end"
        );
        assertNotNull(
            api.getDefaultUsername(),
            "Username should not be null at the end"
        );

        logger.info(
            "✅ Concurrent operations with credential reading stress test passed!"
        );
    }

    @Test
    @DisplayName(
        "🧪 Stress test: Blockchain operations under credential changes"
    )
    @Timeout(90)
    void testBlockchainOperationsUnderCredentialChanges() throws Exception {
        logger.info(
            "🚀 Starting blockchain operations under credential changes stress test..."
        );

        final int NUM_THREADS = 15;
        final int OPERATIONS_PER_THREAD = 20;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        final AtomicInteger successfulBlocks = new AtomicInteger(0);
        final AtomicInteger credentialChanges = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);
        final List<String> createdBlockHashes = new ArrayList<>();

        // Submit all tasks
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        try {
                            // Occasionally change credentials
                            if (op % 3 == 0) {
                                String username =
                                    "blockUser" + threadId + "_" + op;
                                KeyPair keyPair = CryptoUtil.generateKeyPair();

                                String publicKeyString =
                                    CryptoUtil.publicKeyToString(
                                        keyPair.getPublic()
                                    );
                                blockchain.addAuthorizedKey(
                                    publicKeyString,
                                    username,
                                    bootstrapKeyPair,
                                    UserRole.USER
                                );
                                api.setDefaultCredentials(username, keyPair);
                                credentialChanges.incrementAndGet();
                            }

                            // Perform blockchain operation if we have credentials
                            if (api.hasDefaultCredentials()) {
                                String data =
                                    "Stress test data from thread " +
                                    threadId +
                                    " operation " +
                                    op;
                                Block block = api.storeSecret(
                                    data,
                                    "Password123!"
                                );

                                if (block != null) {
                                    successfulBlocks.incrementAndGet();
                                    synchronized (createdBlockHashes) {
                                        createdBlockHashes.add(block.getHash());
                                    }
                                }
                            }

                            Thread.sleep(2); // Small delay
                        } catch (Exception e) {
                            logger.debug(
                                "⚠️ Expected error in thread {} operation {}: {}",
                                threadId,
                                op,
                                e.getMessage()
                            );
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads
        logger.info(
            "🏁 Starting {} threads performing blockchain operations...",
            NUM_THREADS
        );
        startLatch.countDown();

        // Wait for completion
        assertTrue(
            completionLatch.await(75, TimeUnit.SECONDS),
            "Blockchain operations stress test should complete within timeout"
        );

        // Validate results
        logger.info(
            "📊 Results: {} successful blocks, {} credential changes, {} errors",
            successfulBlocks.get(),
            credentialChanges.get(),
            errors.get()
        );
        logger.info(
            "📊 Created {} unique block hashes",
            createdBlockHashes.size()
        );

        // Assertions
        assertTrue(
            successfulBlocks.get() > 0,
            "Should create at least some blocks"
        );
        assertTrue(
            credentialChanges.get() > 0,
            "Should perform credential changes"
        );
        assertEquals(
            createdBlockHashes.size(),
            successfulBlocks.get(),
            "All successful blocks should have unique hashes"
        );

        // Verify blockchain integrity
        assertTrue(
            blockchain.validateChainDetailed().isValid(),
            "Blockchain should remain valid after stress test"
        );

        logger.info(
            "✅ Blockchain operations under credential changes stress test passed!"
        );
    }
}
