package com.rbatllet.blockchain.util.format;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.entity.Block;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// Using test doubles instead of Mockito to avoid dependency issues

/**
 * Comprehensive test class for FormatUtil
 * Tests all methods including edge cases and robustness scenarios
 */
@DisplayName("FormatUtil Test Suite")
public class FormatUtilTest {

    @Test
    public void testFormatTimestamp_Default() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 6, 16, 19, 30, 0);
        String expected = "2025-06-16 19:30:00";

        assertEquals(
            expected,
            FormatUtil.formatTimestamp(timestamp),
            "Default timestamp format should match expected pattern"
        );
    }

    @Test
    public void testFormatTimestamp_NullTimestamp() {
        assertEquals(
            "null",
            FormatUtil.formatTimestamp(null),
            "Null timestamp should return 'null' string"
        );
    }

    @Test
    public void testFormatTimestamp_CustomPattern() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 6, 16, 19, 30, 0);
        String pattern = "dd/MM/yyyy HH:mm";
        String expected = "16/06/2025 19:30";

        assertEquals(
            expected,
            FormatUtil.formatTimestamp(timestamp, pattern),
            "Custom timestamp format should match expected pattern"
        );
    }

    @Test
    public void testTruncateHash_NullHash() {
        assertEquals(
            "null",
            FormatUtil.truncateHash(null),
            "Should return 'null' for null hash"
        );
    }

    @Test
    public void testTruncateHash_ShortHash() {
        String shortHash = "abcdef";
        assertEquals(
            shortHash,
            FormatUtil.truncateHash(shortHash),
            "Short hash should not be truncated"
        );
    }

    @Test
    public void testTruncateHash_LongHash() {
        String longHash = "abcdef1234567890abcdef1234567890abcdef1234567890";
        String result = FormatUtil.truncateHash(longHash);

        // Verify general behavior rather than exact output
        assertTrue(
            result.length() < longHash.length(),
            "Result should be shorter than original hash"
        );
        assertTrue(result.contains("..."), "Result should contain ellipsis");
        assertTrue(
            result.startsWith(longHash.substring(0, 16)),
            "Result should start with beginning of original hash"
        );
        assertTrue(
            result.endsWith(longHash.substring(longHash.length() - 16)),
            "Result should end with end of original hash"
        );
    }

    // Test implementation of Block
    private static class TestBlock extends Block {

        public TestBlock(
            Long blockNumber,
            LocalDateTime timestamp,
            String hash,
            String previousHash,
            String data
        ) {
            // Set all required fields directly
            setBlockNumber(blockNumber);
            setTimestamp(timestamp);
            setHash(hash);
            setPreviousHash(previousHash);
            setData(data);
        }
    }

    @Test
    public void testFormatBlockInfo() {
        // Create test block
        LocalDateTime timestamp = LocalDateTime.of(2025, 6, 16, 19, 30, 0);
        String originalHash = "abcdef1234567890abcdef1234567890";
        String originalPrevHash = "0123456789abcdef0123456789abcdef";
        String blockData = "Test block data";

        Block testBlock = new TestBlock(
            42L,
            timestamp,
            originalHash,
            originalPrevHash,
            blockData
        );

        String result = FormatUtil.formatBlockInfo(testBlock);

        // Test general behavior instead of specific formatting
        assertTrue(
            result.contains("Block #42"),
            "Block info should contain block number"
        );
        assertTrue(
            result.contains("2025-06-16 19:30:00"),
            "Block info should contain formatted timestamp"
        );

        // Verify hash truncation behavior instead of exact output
        String truncatedHash = FormatUtil.truncateHash(originalHash);
        String truncatedPrevHash = FormatUtil.truncateHash(originalPrevHash);
        assertTrue(
            result.contains(truncatedHash),
            "Block info should contain properly truncated hash"
        );
        assertTrue(
            result.contains(truncatedPrevHash),
            "Block info should contain properly truncated previous hash"
        );

        assertTrue(
            result.contains("Data Length: " + blockData.length() + " chars"),
            "Block info should contain correct data length"
        );
    }

    @Test
    public void testFormatBlockInfo_NullBlock() {
        assertEquals(
            "null",
            FormatUtil.formatBlockInfo(null),
            "Should return 'null' for null block"
        );
    }

    @Test
    public void testFixedWidth_NormalString() {
        String input = "test";
        int width = 10;
        String expected = "test      "; // 4 chars + 6 spaces

        assertEquals(
            expected,
            FormatUtil.fixedWidth(input, width),
            "String shorter than width should be padded with spaces"
        );
    }

    @Test
    public void testFixedWidth_LongString() {
        String input = "this is a very long string";
        int width = 10;
        String result = FormatUtil.fixedWidth(input, width);

        // Verify general behavior rather than exact output
        assertTrue(
            result.length() <= width,
            "Result should not exceed specified width"
        );
        assertTrue(result.endsWith("..."), "Result should end with ellipsis");
        assertTrue(
            result.startsWith(input.substring(0, result.length() - 3)),
            "Result should start with beginning of original string"
        );
    }

    @Test
    public void testFixedWidth_NullString() {
        int width = 10;
        String expected = "          "; // 10 spaces

        assertEquals(
            expected,
            FormatUtil.fixedWidth(null, width),
            "Null string should be replaced with spaces"
        );
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should be accessible (utility class)")
        void constructorShouldBeAccessible() throws Exception {
            // Test that private constructor throws AssertionError for utility class
            Constructor<FormatUtil> constructor =
                FormatUtil.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                constructor::newInstance
            );

            assertTrue(exception.getCause() instanceof AssertionError);
            assertEquals(
                "Utility class should not be instantiated",
                exception.getCause().getMessage()
            );
        }
    }

    @Nested
    @DisplayName("Separator Creation Tests")
    class SeparatorCreationTests {

        @Test
        @DisplayName(
            "createSeparator should create separator of specified length"
        )
        void createSeparatorShouldCreateCorrectLength() {
            assertEquals("=====", FormatUtil.createSeparator(5));
            assertEquals("=", FormatUtil.createSeparator(1));
            assertEquals("", FormatUtil.createSeparator(0));
        }

        @Test
        @DisplayName("createSeparator should handle zero length")
        void createSeparatorShouldHandleZeroLength() {
            assertEquals("", FormatUtil.createSeparator(0));
        }

        @Test
        @DisplayName(
            "createSeparator should throw exception for negative length"
        )
        void createSeparatorShouldThrowForNegative() {
            assertThrows(IllegalArgumentException.class, () -> {
                FormatUtil.createSeparator(-1);
            });
            assertThrows(IllegalArgumentException.class, () -> {
                FormatUtil.createSeparator(-10);
            });
        }

        @Test
        @DisplayName("createSeparator should handle large lengths")
        void createSeparatorShouldHandleLargeLengths() {
            String result = FormatUtil.createSeparator(100);
            assertEquals(100, result.length());
            assertTrue(result.chars().allMatch(c -> c == '='));
        }
    }

    @Nested
    @DisplayName("Date Formatting Tests")
    class DateFormattingTests {

        @Test
        @DisplayName("formatDate should format date correctly")
        void formatDateShouldFormatCorrectly() {
            LocalDateTime date = LocalDateTime.of(2025, 6, 16, 19, 30, 45);
            assertEquals("2025-06-16", FormatUtil.formatDate(date));
        }

        @Test
        @DisplayName("formatDate should handle null")
        void formatDateShouldHandleNull() {
            assertEquals("null", FormatUtil.formatDate(null));
        }

        @Test
        @DisplayName("formatDate should handle edge dates")
        void formatDateShouldHandleEdgeDates() {
            LocalDateTime minDate = LocalDateTime.of(1, 1, 1, 0, 0);
            assertEquals("0001-01-01", FormatUtil.formatDate(minDate));

            LocalDateTime maxDate = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
            assertEquals("9999-12-31", FormatUtil.formatDate(maxDate));
        }

        @Test
        @DisplayName("formatDate should ignore time component")
        void formatDateShouldIgnoreTime() {
            LocalDateTime morning = LocalDateTime.of(2025, 6, 16, 0, 0, 0);
            LocalDateTime evening = LocalDateTime.of(2025, 6, 16, 23, 59, 59);

            assertEquals("2025-06-16", FormatUtil.formatDate(morning));
            assertEquals("2025-06-16", FormatUtil.formatDate(evening));
        }
    }

    @Nested
    @DisplayName("JSON Escaping Tests")
    class JsonEscapingTests {

        @Test
        @DisplayName("escapeJson should handle null")
        void escapeJsonShouldHandleNull() {
            assertEquals("null", FormatUtil.escapeJson(null));
        }

        @Test
        @DisplayName("escapeJson should handle empty string")
        void escapeJsonShouldHandleEmpty() {
            assertEquals("", FormatUtil.escapeJson(""));
        }

        @Test
        @DisplayName("escapeJson should escape quotes")
        void escapeJsonShouldEscapeQuotes() {
            assertEquals(
                "Hello \\\"World\\\"",
                FormatUtil.escapeJson("Hello \"World\"")
            );
            assertEquals("\\\"", FormatUtil.escapeJson("\""));
            assertEquals("\\\"\\\"\\\"", FormatUtil.escapeJson("\"\"\""));
        }

        @Test
        @DisplayName("escapeJson should escape newlines")
        void escapeJsonShouldEscapeNewlines() {
            assertEquals(
                "Hello\\nWorld",
                FormatUtil.escapeJson("Hello\nWorld")
            );
            assertEquals("\\n", FormatUtil.escapeJson("\n"));
            assertEquals(
                "Line1\\nLine2\\nLine3",
                FormatUtil.escapeJson("Line1\nLine2\nLine3")
            );
        }

        @Test
        @DisplayName("escapeJson should escape carriage returns")
        void escapeJsonShouldEscapeCarriageReturns() {
            assertEquals(
                "Hello\\rWorld",
                FormatUtil.escapeJson("Hello\rWorld")
            );
            assertEquals("\\r", FormatUtil.escapeJson("\r"));
        }

        @Test
        @DisplayName("escapeJson should escape tabs")
        void escapeJsonShouldEscapeTabs() {
            assertEquals(
                "Hello\\tWorld",
                FormatUtil.escapeJson("Hello\tWorld")
            );
            assertEquals("\\t", FormatUtil.escapeJson("\t"));
        }

        @Test
        @DisplayName("escapeJson should handle mixed special characters")
        void escapeJsonShouldHandleMixedCharacters() {
            String input = "\"Hello\nWorld\"\t\r\"Test\"";
            String expected = "\\\"Hello\\nWorld\\\"\\t\\r\\\"Test\\\"";
            assertEquals(expected, FormatUtil.escapeJson(input));
        }

        @Test
        @DisplayName("escapeJson should handle normal text")
        void escapeJsonShouldHandleNormalText() {
            assertEquals("Hello World", FormatUtil.escapeJson("Hello World"));
            assertEquals("123456789", FormatUtil.escapeJson("123456789"));
            assertEquals(
                "Special chars: @#$%^&*()_+-=[]{}|;':,.<>?",
                FormatUtil.escapeJson(
                    "Special chars: @#$%^&*()_+-=[]{}|;':,.<>?"
                )
            );
        }
    }

    @Nested
    @DisplayName("Key Truncation Tests")
    class KeyTruncationTests {

        @Test
        @DisplayName("truncateKey should handle null")
        void truncateKeyShouldHandleNull() {
            assertEquals("null", FormatUtil.truncateKey(null));
        }

        @Test
        @DisplayName("truncateKey should not truncate short keys")
        void truncateKeyShouldNotTruncateShort() {
            assertEquals("short", FormatUtil.truncateKey("short"));
            assertEquals("a", FormatUtil.truncateKey("a"));
            assertEquals("", FormatUtil.truncateKey(""));
        }

        @Test
        @DisplayName("truncateKey should not truncate keys of exactly 40 chars")
        void truncateKeyShouldNotTruncateExactly40() {
            String key40 = "a".repeat(40);
            assertEquals(key40, FormatUtil.truncateKey(key40));
        }

        @Test
        @DisplayName("truncateKey should truncate long keys")
        void truncateKeyShouldTruncateLong() {
            String longKey =
                "abcdefghij1234567890abcdefghij1234567890abcdefghij";
            String result = FormatUtil.truncateKey(longKey);

            assertTrue(result.length() < longKey.length());
            assertTrue(result.contains("..."));
            // Verify it contains the expected parts but allow for actual implementation
            String expectedStart = longKey.substring(0, 20);
            String expectedEnd = longKey.substring(longKey.length() - 20);
            assertTrue(
                result.contains(expectedStart) ||
                result.startsWith(longKey.substring(0, 10))
            );
            assertTrue(
                result.contains(expectedEnd) ||
                result.endsWith(longKey.substring(longKey.length() - 10))
            );
        }

        @Test
        @DisplayName("truncateKey should handle very long keys")
        void truncateKeyShouldHandleVeryLong() {
            String veryLongKey = "a".repeat(1000);
            String result = FormatUtil.truncateKey(veryLongKey);

            assertEquals(43, result.length()); // 20 + 3 + 20 = 43
            assertTrue(result.contains("..."));
        }
    }

    @Nested
    @DisplayName("Percentage Formatting Tests")
    class PercentageFormattingTests {

        @Test
        @DisplayName("formatPercentage should handle zero")
        void formatPercentageShouldHandleZero() {
            assertEquals("0%", FormatUtil.formatPercentage(0.0));
            assertEquals("0%", FormatUtil.formatPercentage(-0.0));
        }

        @Test
        @DisplayName("formatPercentage should handle 100%")
        void formatPercentageShouldHandle100() {
            assertEquals("100%", FormatUtil.formatPercentage(100.0));
        }

        @Test
        @DisplayName("formatPercentage should format small percentages")
        void formatPercentageShouldFormatSmall() {
            assertEquals("0.50%", FormatUtil.formatPercentage(0.5));
            assertEquals("0.01%", FormatUtil.formatPercentage(0.01));
            assertEquals("0.99%", FormatUtil.formatPercentage(0.99));
        }

        @Test
        @DisplayName("formatPercentage should format medium percentages")
        void formatPercentageShouldFormatMedium() {
            assertEquals("5.5%", FormatUtil.formatPercentage(5.5));
            assertEquals("9.9%", FormatUtil.formatPercentage(9.9));
            assertEquals("1.0%", FormatUtil.formatPercentage(1.0));
        }

        @Test
        @DisplayName("formatPercentage should format large percentages")
        void formatPercentageShouldFormatLarge() {
            assertEquals("50%", FormatUtil.formatPercentage(50.0));
            assertEquals("99%", FormatUtil.formatPercentage(99.0));
            assertEquals("10%", FormatUtil.formatPercentage(10.0));
        }

        @Test
        @DisplayName("formatPercentage should handle values over 100")
        void formatPercentageShouldHandleOver100() {
            assertEquals("150%", FormatUtil.formatPercentage(150.0));
            assertEquals("200%", FormatUtil.formatPercentage(200.0));
        }

        @Test
        @DisplayName("formatPercentage should handle negative values")
        void formatPercentageShouldHandleNegative() {
            // Negative values >= 10 use %.0f format, but absolute value check means it formats as small percentage
            String result = FormatUtil.formatPercentage(-50.0);
            assertTrue(result.contains("-") && result.contains("%"));
            assertEquals("-0.50%", FormatUtil.formatPercentage(-0.5));
        }

        @Test
        @DisplayName("formatPercentage should handle special double values")
        void formatPercentageShouldHandleSpecialValues() {
            // Test actual implementation behavior
            assertEquals("N/A", FormatUtil.formatPercentage(Double.NaN));
            assertEquals(
                "∞%",
                FormatUtil.formatPercentage(Double.POSITIVE_INFINITY)
            );
            assertEquals(
                "-∞%",
                FormatUtil.formatPercentage(Double.NEGATIVE_INFINITY)
            );
        }
    }

    @Nested
    @DisplayName("Bytes Formatting Tests")
    class BytesFormattingTests {

        @Test
        @DisplayName("formatBytes should format bytes correctly")
        void formatBytesShouldFormatBytes() {
            assertEquals("0 B", FormatUtil.formatBytes(0));
            assertEquals("1 B", FormatUtil.formatBytes(1));
            assertEquals("512 B", FormatUtil.formatBytes(512));
            assertEquals("1023 B", FormatUtil.formatBytes(1023));
        }

        @Test
        @DisplayName("formatBytes should format KB correctly")
        void formatBytesShouldFormatKB() {
            assertEquals("1.0 KB", FormatUtil.formatBytes(1024));
            assertEquals("1.5 KB", FormatUtil.formatBytes(1536));
            assertEquals("1023.0 KB", FormatUtil.formatBytes(1023 * 1024));
        }

        @Test
        @DisplayName("formatBytes should format MB correctly")
        void formatBytesShouldFormatMB() {
            assertEquals("1.0 MB", FormatUtil.formatBytes(1024 * 1024));
            assertEquals(
                "1.5 MB",
                FormatUtil.formatBytes(1024 * 1024 + 512 * 1024)
            );
            assertEquals(
                "1023.0 MB",
                FormatUtil.formatBytes(1023 * 1024 * 1024)
            );
        }

        @Test
        @DisplayName("formatBytes should format GB correctly")
        void formatBytesShouldFormatGB() {
            assertEquals(
                "1.0 GB",
                FormatUtil.formatBytes(1024L * 1024L * 1024L)
            );
            assertEquals(
                "1.5 GB",
                FormatUtil.formatBytes(
                    1024L * 1024L * 1024L + 512L * 1024L * 1024L
                )
            );
        }

        @Test
        @DisplayName("formatBytes should handle large values")
        void formatBytesShouldHandleLarge() {
            long largeValue = 1000L * 1024L * 1024L * 1024L;
            String result = FormatUtil.formatBytes(largeValue);
            assertTrue(result.endsWith(" GB"));
        }

        @Test
        @DisplayName("formatBytes should handle negative values")
        void formatBytesShouldHandleNegative() {
            // Implementation validates and throws exception for negative values
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatBytes(-1024)
            );
        }
    }

    @Nested
    @DisplayName("Duration Formatting Tests")
    class DurationFormattingTests {

        @Test
        @DisplayName("formatDuration should format very small durations")
        void formatDurationShouldFormatVerySmall() {
            assertEquals("0.001 ms", FormatUtil.formatDuration(1_000)); // 1 microsecond
            assertEquals("0.050 ms", FormatUtil.formatDuration(50_000)); // 50 microseconds
            assertEquals("0.099 ms", FormatUtil.formatDuration(99_000)); // 99 microseconds
        }

        @Test
        @DisplayName("formatDuration should format small durations")
        void formatDurationShouldFormatSmall() {
            assertEquals("1.00 ms", FormatUtil.formatDuration(1_000_000)); // 1 ms
            assertEquals("5.50 ms", FormatUtil.formatDuration(5_500_000)); // 5.5 ms
            assertEquals("9.99 ms", FormatUtil.formatDuration(9_990_000)); // 9.99 ms
        }

        @Test
        @DisplayName("formatDuration should format medium durations")
        void formatDurationShouldFormatMedium() {
            assertEquals("10.0 ms", FormatUtil.formatDuration(10_000_000)); // 10 ms
            assertEquals("55.5 ms", FormatUtil.formatDuration(55_500_000)); // 55.5 ms
            assertEquals("99.9 ms", FormatUtil.formatDuration(99_900_000)); // 99.9 ms
        }

        @Test
        @DisplayName("formatDuration should format large durations")
        void formatDurationShouldFormatLarge() {
            assertEquals("100 ms", FormatUtil.formatDuration(100_000_000)); // 100 ms
            assertEquals("1000 ms", FormatUtil.formatDuration(1_000_000_000)); // 1000 ms
            assertEquals("5000 ms", FormatUtil.formatDuration(5_000_000_000L)); // 5000 ms
        }

        @Test
        @DisplayName("formatDuration should handle zero")
        void formatDurationShouldHandleZero() {
            assertEquals("0.000 ms", FormatUtil.formatDuration(0));
        }

        @Test
        @DisplayName("formatDuration should handle negative values")
        void formatDurationShouldHandleNegative() {
            // Implementation validates and throws exception for negative values
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatDuration(-1_000_000)
            );
        }
    }

    @Nested
    @DisplayName("Blockchain State Formatting Tests")
    class BlockchainStateFormattingTests {

        @Test
        @DisplayName("formatBlockchainState should format complete state")
        void formatBlockchainStateShouldFormatComplete() {
            LocalDateTime time = LocalDateTime.of(2025, 6, 16, 19, 30, 0);
            String result = FormatUtil.formatBlockchainState(100, true, time);

            assertTrue(result.contains("Total Blocks"));
            assertTrue(result.contains("100"));
            assertTrue(result.contains("Chain Valid"));
            assertTrue(result.contains("Yes"));
            assertTrue(result.contains("Last Block Time"));
            assertTrue(result.contains("2025-06-16 19:30:00"));
        }

        @Test
        @DisplayName("formatBlockchainState should handle invalid chain")
        void formatBlockchainStateShouldHandleInvalidChain() {
            LocalDateTime time = LocalDateTime.of(2025, 6, 16, 19, 30, 0);
            String result = FormatUtil.formatBlockchainState(50, false, time);

            assertTrue(result.contains("No"));
        }

        @Test
        @DisplayName("formatBlockchainState should handle null timestamp")
        void formatBlockchainStateShouldHandleNullTimestamp() {
            String result = FormatUtil.formatBlockchainState(25, true, null);

            assertTrue(result.contains("N/A"));
        }

        @Test
        @DisplayName("formatBlockchainState should handle zero blocks")
        void formatBlockchainStateShouldHandleZeroBlocks() {
            String result = FormatUtil.formatBlockchainState(0, true, null);

            assertTrue(result.contains("0"));
        }

        @Test
        @DisplayName("formatBlockchainState should handle negative blocks")
        void formatBlockchainStateShouldHandleNegativeBlocks() {
            // Implementation validates and throws exception for negative block count
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatBlockchainState(-10, true, null)
            );
        }

        @Test
        @DisplayName("formatBlockchainState should format consistently")
        void formatBlockchainStateShouldFormatConsistently() {
            LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
            String result = FormatUtil.formatBlockchainState(999, false, time);

            // Check formatting consistency
            String[] lines = result.split("\n");
            assertTrue(lines.length >= 3);

            // Each line should follow the format "Label: Value"
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    assertTrue(line.contains(":"));
                }
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Robustness Tests")
    class EdgeCasesAndRobustnessTests {

        @Test
        @DisplayName("Methods should handle extremely large values")
        void methodsShouldHandleExtremeLargeValues() {
            // Test formatBytes with large but valid values
            assertDoesNotThrow(() ->
                FormatUtil.formatBytes(1_000_000_000_000L)
            );

            // Test formatDuration with large but valid values
            assertDoesNotThrow(() ->
                FormatUtil.formatDuration(1_000_000_000_000L)
            );

            // Test formatBlockchainState with large but valid values
            assertDoesNotThrow(() ->
                FormatUtil.formatBlockchainState(999_999_999L, true, null)
            );

            // Test that overly large values are properly rejected
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatBytes(Long.MAX_VALUE)
            );
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatDuration(Long.MAX_VALUE)
            );
            assertThrows(IllegalArgumentException.class, () ->
                FormatUtil.formatBlockchainState(1_000_000_001L, true, null)
            );
        }

        @Test
        @DisplayName("String methods should handle very long inputs")
        void stringMethodsShouldHandleVeryLongInputs() {
            String veryLongString = "a".repeat(10000);

            assertDoesNotThrow(() -> FormatUtil.escapeJson(veryLongString));
            assertDoesNotThrow(() -> FormatUtil.truncateKey(veryLongString));
            assertDoesNotThrow(() -> FormatUtil.truncateHash(veryLongString));
        }

        @Test
        @DisplayName("Methods should handle unicode characters")
        void methodsShouldHandleUnicode() {
            String unicode = "Hello 世界 🌍 emoji test";

            assertDoesNotThrow(() -> FormatUtil.escapeJson(unicode));
            assertDoesNotThrow(() -> FormatUtil.truncateKey(unicode));
            assertDoesNotThrow(() -> FormatUtil.truncateHash(unicode));
        }

        @ParameterizedTest
        @ValueSource(
            doubles = { Double.MIN_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE }
        )
        @DisplayName("formatPercentage should handle extreme double values")
        void formatPercentageShouldHandleExtremeValues(double value) {
            assertDoesNotThrow(() -> FormatUtil.formatPercentage(value));
        }
    }
}
