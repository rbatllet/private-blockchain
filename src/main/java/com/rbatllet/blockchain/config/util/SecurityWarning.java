package com.rbatllet.blockchain.config.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a security warning or concern detected in database configuration.
 *
 * <p>This class provides a structured way to communicate security issues to the
 * presentation layer (CLI, web, API) without dictating how they should be displayed.
 * Each warning includes severity, message, and optional remediation steps.</p>
 *
 * <p><b>Design Principle:</b> Business logic (CORE) detects issues and creates warnings,
 * presentation layer (CLI/Web) decides how to display them.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * SecurityWarning warning = SecurityWarning.builder()
 *     .severity(SecurityWarning.Severity.HIGH)
 *     .message("Database password loaded from configuration file")
 *     .addRemediationStep("Use DB_PASSWORD environment variable instead")
 *     .addRemediationStep("Set file permissions to 600 (rw-------)")
 *     .build();
 *
 * // CLI would display this as console output
 * // Web would show as a banner
 * // API would include in JSON response
 * }</pre>
 *
 * @since 1.0.6
 * @see ConfigurationSecurityAnalyzer
 */
public final class SecurityWarning {

    /**
     * Severity levels for security warnings.
     *
     * <p>Severity indicates the importance and urgency of addressing the issue:</p>
     * <ul>
     *   <li><b>CRITICAL</b>: Immediate action required, severe security risk</li>
     *   <li><b>HIGH</b>: Should be addressed soon, significant security concern</li>
     *   <li><b>MEDIUM</b>: Should be reviewed, moderate security concern</li>
     *   <li><b>LOW</b>: Optional improvement, minor security concern</li>
     *   <li><b>INFO</b>: Informational only, no immediate action needed</li>
     * </ul>
     */
    public enum Severity {
        /**
         * Critical severity - immediate action required.
         * Examples: Passwords in plaintext files with world-readable permissions.
         */
        CRITICAL,

        /**
         * High severity - should be addressed soon.
         * Examples: Passwords in files (even with secure permissions), weak encryption.
         */
        HIGH,

        /**
         * Medium severity - should be reviewed.
         * Examples: Usernames in configuration files, non-standard ports exposed.
         */
        MEDIUM,

        /**
         * Low severity - optional improvement.
         * Examples: Missing connection timeouts, suboptimal pool sizes.
         */
        LOW,

        /**
         * Informational - no immediate action needed.
         * Examples: Configuration loaded from environment variables (good practice).
         */
        INFO
    }

    private final Severity severity;
    private final String message;
    private final List<String> remediationSteps;
    private final String code;
    private final Instant timestamp;

    /**
     * Private constructor. Use {@link Builder} to create instances.
     */
    private SecurityWarning(Builder builder) {
        this.severity = Objects.requireNonNull(builder.severity, "Severity cannot be null");
        this.message = Objects.requireNonNull(builder.message, "Message cannot be null");
        this.remediationSteps = Collections.unmodifiableList(new ArrayList<>(builder.remediationSteps));
        this.code = builder.code;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    }

    /**
     * Gets the severity level of this warning.
     *
     * @return the severity level
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Gets the warning message.
     *
     * @return the warning message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the list of remediation steps to address this warning.
     *
     * @return unmodifiable list of remediation steps (may be empty)
     */
    public List<String> getRemediationSteps() {
        return remediationSteps;
    }

    /**
     * Gets the warning code (optional identifier for programmatic handling).
     *
     * @return the warning code, or null if not set
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the timestamp when this warning was created.
     *
     * @return the creation timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Creates a new builder for constructing SecurityWarning instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple warning with severity and message only.
     *
     * @param severity the severity level
     * @param message the warning message
     * @return a new SecurityWarning
     */
    public static SecurityWarning of(Severity severity, String message) {
        return builder()
            .severity(severity)
            .message(message)
            .build();
    }

    /**
     * Creates a critical severity warning.
     *
     * @param message the warning message
     * @return a new SecurityWarning with CRITICAL severity
     */
    public static SecurityWarning critical(String message) {
        return of(Severity.CRITICAL, message);
    }

    /**
     * Creates a high severity warning.
     *
     * @param message the warning message
     * @return a new SecurityWarning with HIGH severity
     */
    public static SecurityWarning high(String message) {
        return of(Severity.HIGH, message);
    }

    /**
     * Creates a medium severity warning.
     *
     * @param message the warning message
     * @return a new SecurityWarning with MEDIUM severity
     */
    public static SecurityWarning medium(String message) {
        return of(Severity.MEDIUM, message);
    }

    /**
     * Creates a low severity warning.
     *
     * @param message the warning message
     * @return a new SecurityWarning with LOW severity
     */
    public static SecurityWarning low(String message) {
        return of(Severity.LOW, message);
    }

    /**
     * Creates an informational warning.
     *
     * @param message the warning message
     * @return a new SecurityWarning with INFO severity
     */
    public static SecurityWarning info(String message) {
        return of(Severity.INFO, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityWarning that = (SecurityWarning) o;
        return severity == that.severity &&
               Objects.equals(message, that.message) &&
               Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(severity, message, code);
    }

    @Override
    public String toString() {
        return "SecurityWarning{" +
               "severity=" + severity +
               ", message='" + message + '\'' +
               ", code='" + code + '\'' +
               ", remediationSteps=" + remediationSteps.size() +
               ", timestamp=" + timestamp +
               '}';
    }

    /**
     * Builder for constructing SecurityWarning instances with a fluent API.
     */
    public static final class Builder {
        private Severity severity;
        private String message;
        private final List<String> remediationSteps = new ArrayList<>();
        private String code;
        private Instant timestamp;

        private Builder() {
        }

        /**
         * Sets the severity level.
         *
         * @param severity the severity level (required)
         * @return this builder
         */
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        /**
         * Sets the warning message.
         *
         * @param message the warning message (required)
         * @return this builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Adds a remediation step.
         *
         * @param step the remediation step
         * @return this builder
         */
        public Builder addRemediationStep(String step) {
            if (step != null && !step.isEmpty()) {
                this.remediationSteps.add(step);
            }
            return this;
        }

        /**
         * Adds multiple remediation steps.
         *
         * @param steps the remediation steps
         * @return this builder
         */
        public Builder addRemediationSteps(List<String> steps) {
            if (steps != null) {
                steps.forEach(this::addRemediationStep);
            }
            return this;
        }

        /**
         * Sets the warning code (optional identifier).
         *
         * @param code the warning code
         * @return this builder
         */
        public Builder code(String code) {
            this.code = code;
            return this;
        }

        /**
         * Sets the timestamp (optional, defaults to now).
         *
         * @param timestamp the timestamp
         * @return this builder
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds the SecurityWarning instance.
         *
         * @return a new SecurityWarning
         * @throws NullPointerException if severity or message is null
         */
        public SecurityWarning build() {
            return new SecurityWarning(this);
        }
    }
}
