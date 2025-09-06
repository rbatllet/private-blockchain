package com.rbatllet.blockchain.util.format;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.entity.Block;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Additional robustness tests for FormatUtil
 * Tests the enhanced validation and error handling capabilities
 */
@DisplayName("FormatUtil Robustness Test Suite")
public class FormatUtilRobustnessTest {

    @Nested
    @DisplayName("Enhanced Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("formatTimestamp should reject overly long patterns")
        void formatTimestampShouldRejectLongPatterns() {
            String longPattern = "y".repeat(1001);
            LocalDateTime now = LocalDateTime.now();

            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatTimestamp(now, longPattern)
            );
        }

        @Test
        @DisplayName("fixedWidth should reject excessive width")
        void fixedWidthShouldRejectExcessiveWidth() {
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.fixedWidth("test", 10001)
            );
        }

        @Test
        @DisplayName("formatBytes should handle very large values")
        void formatBytesShouldHandleVeryLargeValues() {
            // Test TB formatting
            long terabytes = 5L * 1024L * 1024L * 1024L * 1024L;
            String result = FormatUtil.formatBytes(terabytes);
            assertTrue(result.contains("TB"));
        }

        @Test
        @DisplayName("formatBytes should reject overly large values")
        void formatBytesShouldRejectOverlyLargeValues() {
            long tooLarge = Long.MAX_VALUE;
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatBytes(tooLarge)
            );
        }

        @Test
        @DisplayName("formatDuration should reject overly large values")
        void formatDurationShouldRejectOverlyLargeValues() {
            long tooLarge = Long.MAX_VALUE;
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatDuration(tooLarge)
            );
        }

        @Test
        @DisplayName(
            "formatBlockchainState should reject excessive block counts"
        )
        void formatBlockchainStateShouldRejectExcessiveBlockCounts() {
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatBlockchainState(1_000_000_001L, true, null)
            );
        }
    }

    @Nested
    @DisplayName("Enhanced JSON Escaping Tests")
    class JsonEscapingTests {

        @Test
        @DisplayName("escapeJson should handle control characters")
        void escapeJsonShouldHandleControlCharacters() {
            String controlChars = "\u0000\u0001\u001F";
            String result = FormatUtil.escapeJson(controlChars);

            assertTrue(result.contains("\\u0000"));
            assertTrue(result.contains("\\u0001"));
            assertTrue(result.contains("\\u001f"));
        }

        @Test
        @DisplayName("escapeJson should handle mixed special characters")
        void escapeJsonShouldHandleMixedSpecialCharacters() {
            String mixed = "Hello\nWorld\"\t\u0000Test";
            String result = FormatUtil.escapeJson(mixed);

            assertTrue(result.contains("\\n"));
            assertTrue(result.contains("\\\""));
            assertTrue(result.contains("\\t"));
            assertTrue(result.contains("\\u0000"));
        }
    }

    @Nested
    @DisplayName("Enhanced Percentage Formatting Tests")
    class PercentageFormattingTests {

        @Test
        @DisplayName("formatPercentage should handle very large values")
        void formatPercentageShouldHandleVeryLargeValues() {
            double veryLarge = 2_000_000.0;
            String result = FormatUtil.formatPercentage(veryLarge);
            assertEquals(">1M%", result);
        }

        @Test
        @DisplayName("formatPercentage should handle very small values")
        void formatPercentageShouldHandleVerySmallValues() {
            double verySmall = 0.001;
            String result = FormatUtil.formatPercentage(verySmall);
            assertEquals("0.001%", result);
        }

        @Test
        @DisplayName(
            "formatPercentage should handle negative very large values"
        )
        void formatPercentageShouldHandleNegativeVeryLargeValues() {
            double negativeVeryLarge = -2_000_000.0;
            String result = FormatUtil.formatPercentage(negativeVeryLarge);
            assertEquals("<-1M%", result);
        }
    }

    @Nested
    @DisplayName("Enhanced Block Formatting Tests")
    class BlockFormattingTests {

        @Test
        @DisplayName(
            "formatBlockInfo should handle block with invalid block number"
        )
        void formatBlockInfoShouldHandleInvalidBlockNumber() {
            Block invalidBlock = new TestBlock(-5L, null, null, null, null);
            String result = FormatUtil.formatBlockInfo(invalidBlock);

            assertTrue(result.contains("INVALID(-5)"));
        }

        @Test
        @DisplayName(
            "formatBlockInfo should handle block with invalid timestamp"
        )
        void formatBlockInfoShouldHandleInvalidTimestamp() {
            LocalDateTime invalidDate = LocalDateTime.of(1800, 1, 1, 0, 0);
            Block invalidBlock = new TestBlock(
                1L,
                invalidDate,
                null,
                null,
                null
            );
            String result = FormatUtil.formatBlockInfo(invalidBlock);

            assertTrue(result.contains("INVALID/NULL"));
        }

        @Test
        @DisplayName("formatBlockInfo should handle block with missing hashes")
        void formatBlockInfoShouldHandleMissingHashes() {
            Block blockWithoutHashes = new TestBlock(
                1L,
                LocalDateTime.now(),
                "",
                "",
                "data"
            );
            String result = FormatUtil.formatBlockInfo(blockWithoutHashes);

            assertTrue(result.contains("Hash: MISSING"));
            assertTrue(result.contains("Previous Hash: MISSING"));
        }

        @Test
        @DisplayName("formatBlockInfo should warn about very large data")
        void formatBlockInfoShouldWarnAboutVeryLargeData() {
            String largeData = "x".repeat(1_500_000);
            Block blockWithLargeData = new TestBlock(
                1L,
                LocalDateTime.now(),
                "hash",
                "prevhash",
                largeData
            );
            String result = FormatUtil.formatBlockInfo(blockWithLargeData);

            assertTrue(result.contains("WARNING: Very large"));
        }

        @Test
        @DisplayName("formatBlockInfo should handle exceptions gracefully")
        void formatBlockInfoShouldHandleExceptionsGracefully() {
            Block problematicBlock = new ProblematicBlock();
            String result = FormatUtil.formatBlockInfo(problematicBlock);

            assertTrue(result.startsWith("Error formatting block:"));
        }
    }

    @Nested
    @DisplayName("Enhanced Blockchain State Tests")
    class BlockchainStateTests {

        @Test
        @DisplayName(
            "formatBlockchainState should handle invalid timestamps gracefully"
        )
        void formatBlockchainStateShouldHandleInvalidTimestamps() {
            LocalDateTime invalidDate = LocalDateTime.of(1800, 1, 1, 0, 0);
            String result = FormatUtil.formatBlockchainState(
                10,
                true,
                invalidDate
            );

            assertTrue(result.contains("INVALID DATE"));
        }

        @Test
        @DisplayName(
            "formatBlockchainState should format large block counts with commas"
        )
        void formatBlockchainStateShouldFormatLargeBlockCounts() {
            String result = FormatUtil.formatBlockchainState(
                1_234_567,
                true,
                null
            );

            assertTrue(result.contains("1,234,567"));
        }

        @Test
        @DisplayName(
            "formatBlockchainState should handle exceptions gracefully"
        )
        void formatBlockchainStateShouldHandleExceptionsGracefully() {
            // This test ensures the method doesn't crash even with edge cases
            String result = FormatUtil.formatBlockchainState(
                0,
                true,
                LocalDateTime.now()
            );

            assertFalse(result.startsWith("Error"));
        }
    }

    // Test helper classes
    private static class TestBlock extends Block {

        private final Long blockNumber;
        private final LocalDateTime timestamp;
        private final String hash;
        private final String previousHash;
        private final String data;

        public TestBlock(
            Long blockNumber,
            LocalDateTime timestamp,
            String hash,
            String previousHash,
            String data
        ) {
            this.blockNumber = blockNumber;
            this.timestamp = timestamp;
            this.hash = hash;
            this.previousHash = previousHash;
            this.data = data;
        }

        @Override
        public Long getBlockNumber() {
            return blockNumber;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String getHash() {
            return hash;
        }

        @Override
        public String getPreviousHash() {
            return previousHash;
        }

        @Override
        public String getData() {
            return data;
        }
    }

    private static class ProblematicBlock extends Block {

        @Override
        public Long getBlockNumber() {
            throw new RuntimeException("Simulated error");
        }

        @Override
        public LocalDateTime getTimestamp() {
            return null;
        }

        @Override
        public String getHash() {
            return null;
        }

        @Override
        public String getPreviousHash() {
            return null;
        }

        @Override
        public String getData() {
            return null;
        }
    }
}
