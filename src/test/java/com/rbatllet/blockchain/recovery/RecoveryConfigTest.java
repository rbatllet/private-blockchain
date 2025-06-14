package com.rbatllet.blockchain.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RecoveryConfig functionality
 */
@DisplayName("Recovery Configuration Tests")
class RecoveryConfigTest {

    @Nested
    @DisplayName("Default Configuration")
    class DefaultConfiguration {

        @Test
        @DisplayName("Should have sensible defaults")
        void shouldHaveSensibleDefaults() {
            RecoveryConfig config = new RecoveryConfig();
            
            assertTrue(config.isReauthorizationEnabled());
            assertTrue(config.isRollbackEnabled());
            assertTrue(config.isPartialExportEnabled());
            assertTrue(config.isAutoRecoveryEnabled());
            assertTrue(config.isAuditLoggingEnabled());
            assertFalse(config.isVerboseLogging());
            
            assertEquals("2.0", config.getRecoveryVersion());
            assertEquals("RECOVERED", config.getRecoveryOwnerSuffix());
            assertEquals(100, config.getMaxOwnerNameLength());
            assertEquals(3, config.getMaxRecoveryAttempts());
            assertEquals(30000, config.getRecoveryTimeoutMs());
            assertEquals(100, config.getMaxRollbackBlocks());
            assertEquals(0.1, config.getRollbackSafetyMargin(), 0.001);
            
            assertTrue(config.isValid());
        }
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {

        @Test
        @DisplayName("Should build config with custom values")
        void shouldBuildConfigWithCustomValues() {
            RecoveryConfig config = RecoveryConfig.builder()
                .enableReauthorization(false)
                .enableRollback(true)
                .enablePartialExport(false)
                .enableAutoRecovery(false)
                .withAuditLogging(false, "custom.log")
                .withVerboseLogging(true)
                .withRecoveryLimits(5, 60000)
                .withBackupDirectory("custom_backups")
                .withRollbackLimits(50, 0.2)
                .build();
            
            assertFalse(config.isReauthorizationEnabled());
            assertTrue(config.isRollbackEnabled());
            assertFalse(config.isPartialExportEnabled());
            assertFalse(config.isAutoRecoveryEnabled());
            assertFalse(config.isAuditLoggingEnabled());
            assertTrue(config.isVerboseLogging());
            
            assertEquals("custom.log", config.getAuditLogFile());
            assertEquals(5, config.getMaxRecoveryAttempts());
            assertEquals(60000, config.getRecoveryTimeoutMs());
            assertEquals("custom_backups", config.getBackupDirectory());
            assertEquals(50, config.getMaxRollbackBlocks());
            assertEquals(0.2, config.getRollbackSafetyMargin(), 0.001);
            
            assertTrue(config.isValid());
        }

        @Test
        @DisplayName("Should chain builder methods")
        void shouldChainBuilderMethods() {
            RecoveryConfig config = RecoveryConfig.builder()
                .enableReauthorization(true)
                .enableRollback(true)
                .enablePartialExport(true)
                .enableAutoRecovery(true)
                .build();
            
            assertNotNull(config);
            assertTrue(config.isValid());
        }
    }

    @Nested
    @DisplayName("Preset Configurations")
    class PresetConfigurations {

        @Test
        @DisplayName("Should provide conservative configuration")
        void shouldProvideConservativeConfiguration() {
            RecoveryConfig config = RecoveryConfig.conservativeConfig();
            
            assertTrue(config.isReauthorizationEnabled());
            assertFalse(config.isRollbackEnabled()); // Conservative: no rollback
            assertTrue(config.isPartialExportEnabled());
            assertFalse(config.isAutoRecoveryEnabled()); // Manual only
            
            assertEquals(1, config.getMaxRecoveryAttempts());
            assertEquals(10000, config.getRecoveryTimeoutMs());
            
            assertTrue(config.isValid());
        }

        @Test
        @DisplayName("Should provide aggressive configuration")
        void shouldProvideAggressiveConfiguration() {
            RecoveryConfig config = RecoveryConfig.aggressiveConfig();
            
            assertTrue(config.isReauthorizationEnabled());
            assertTrue(config.isRollbackEnabled());
            assertTrue(config.isPartialExportEnabled());
            assertTrue(config.isAutoRecoveryEnabled());
            
            assertEquals(5, config.getMaxRecoveryAttempts());
            assertEquals(60000, config.getRecoveryTimeoutMs());
            assertEquals(200, config.getMaxRollbackBlocks());
            assertEquals(0.05, config.getRollbackSafetyMargin(), 0.001);
            
            assertTrue(config.isValid());
        }

        @Test
        @DisplayName("Should provide production configuration")
        void shouldProvideProductionConfiguration() {
            RecoveryConfig config = RecoveryConfig.productionConfig();
            
            assertTrue(config.isReauthorizationEnabled());
            assertTrue(config.isRollbackEnabled());
            assertTrue(config.isPartialExportEnabled());
            assertTrue(config.isAutoRecoveryEnabled());
            assertTrue(config.isAuditLoggingEnabled());
            assertFalse(config.isVerboseLogging()); // Production: quiet
            
            assertEquals("production_recovery_audit.log", config.getAuditLogFile());
            assertEquals(3, config.getMaxRecoveryAttempts());
            assertEquals(30000, config.getRecoveryTimeoutMs());
            
            assertTrue(config.isValid());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should validate positive values")
        void shouldValidatePositiveValues() {
            RecoveryConfig invalidConfig1 = RecoveryConfig.builder()
                .withRecoveryLimits(0, 30000) // Invalid: 0 attempts
                .build();
            assertFalse(invalidConfig1.isValid());

            RecoveryConfig invalidConfig2 = RecoveryConfig.builder()
                .withRecoveryLimits(3, 0) // Invalid: 0 timeout
                .build();
            assertFalse(invalidConfig2.isValid());

            RecoveryConfig invalidConfig3 = RecoveryConfig.builder()
                .withRollbackLimits(-1, 0.1) // Invalid: negative blocks
                .build();
            assertFalse(invalidConfig3.isValid());
        }

        @Test
        @DisplayName("Should validate safety margin range")
        void shouldValidateSafetyMarginRange() {
            RecoveryConfig invalidConfig1 = RecoveryConfig.builder()
                .withRollbackLimits(100, -0.1) // Invalid: negative margin
                .build();
            assertFalse(invalidConfig1.isValid());

            RecoveryConfig invalidConfig2 = RecoveryConfig.builder()
                .withRollbackLimits(100, 1.5) // Invalid: > 1.0 margin
                .build();
            assertFalse(invalidConfig2.isValid());

            RecoveryConfig validConfig = RecoveryConfig.builder()
                .withRollbackLimits(100, 0.5) // Valid: 50% margin
                .build();
            assertTrue(validConfig.isValid());
        }
    }

    @Nested
    @DisplayName("Setters and Getters")
    class SettersAndGetters {

        @Test
        @DisplayName("Should allow modification via setters")
        void shouldAllowModificationViaSetters() {
            RecoveryConfig config = new RecoveryConfig();
            
            config.setReauthorizationEnabled(false);
            config.setRollbackEnabled(false);
            config.setPartialExportEnabled(false);
            config.setAutoRecoveryEnabled(false);
            config.setAuditLoggingEnabled(false);
            config.setVerboseLogging(true);
            
            assertFalse(config.isReauthorizationEnabled());
            assertFalse(config.isRollbackEnabled());
            assertFalse(config.isPartialExportEnabled());
            assertFalse(config.isAutoRecoveryEnabled());
            assertFalse(config.isAuditLoggingEnabled());
            assertTrue(config.isVerboseLogging());
        }

        @Test
        @DisplayName("Should update numeric values via setters")
        void shouldUpdateNumericValuesViaSetters() {
            RecoveryConfig config = new RecoveryConfig();
            
            config.setMaxRecoveryAttempts(10);
            config.setRecoveryTimeoutMs(120000);
            config.setMaxRollbackBlocks(500);
            config.setRollbackSafetyMargin(0.25);
            config.setMaxOwnerNameLength(200);
            
            assertEquals(10, config.getMaxRecoveryAttempts());
            assertEquals(120000, config.getRecoveryTimeoutMs());
            assertEquals(500, config.getMaxRollbackBlocks());
            assertEquals(0.25, config.getRollbackSafetyMargin(), 0.001);
            assertEquals(200, config.getMaxOwnerNameLength());
            
            assertTrue(config.isValid());
        }

        @Test
        @DisplayName("Should update string values via setters")
        void shouldUpdateStringValuesViaSetters() {
            RecoveryConfig config = new RecoveryConfig();
            
            config.setRecoveryVersion("3.0");
            config.setAuditLogFile("test_audit.log");
            config.setBackupDirectory("test_backups");
            config.setRecoveryOwnerSuffix("RESTORED");
            
            assertEquals("3.0", config.getRecoveryVersion());
            assertEquals("test_audit.log", config.getAuditLogFile());
            assertEquals("test_backups", config.getBackupDirectory());
            assertEquals("RESTORED", config.getRecoveryOwnerSuffix());
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {

        @Test
        @DisplayName("Should provide meaningful toString")
        void shouldProvideMeaningfulToString() {
            RecoveryConfig config = new RecoveryConfig();
            String str = config.toString();
            
            assertNotNull(str);
            assertTrue(str.contains("RecoveryConfig"));
            assertTrue(str.contains("reauth=true"));
            assertTrue(str.contains("rollback=true"));
            assertTrue(str.contains("export=true"));
            assertTrue(str.contains("auto=true"));
            assertTrue(str.contains("version=2.0"));
        }

        @Test
        @DisplayName("Should show custom values in toString")
        void shouldShowCustomValuesInToString() {
            RecoveryConfig config = RecoveryConfig.builder()
                .enableReauthorization(false)
                .enableAutoRecovery(false)
                .build();
            
            String str = config.toString();
            assertTrue(str.contains("reauth=false"));
            assertTrue(str.contains("auto=false"));
        }
    }
}