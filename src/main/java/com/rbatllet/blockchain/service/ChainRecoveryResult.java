package com.rbatllet.blockchain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive result container for chain recovery operations
 * Provides detailed information about recovery process and results
 */
public class ChainRecoveryResult {

    public enum RecoveryStatus {
        SUCCESS("✅ Recovery Successful", "Chain fully recovered"),
        PARTIAL_SUCCESS(
            "⚠️ Partial Recovery",
            "Some issues remain after recovery"
        ),
        FAILED("❌ Recovery Failed", "Unable to recover chain"),
        NOT_NEEDED("ℹ️ No Recovery Needed", "Chain is already healthy"),
        IN_PROGRESS("🔄 Recovery In Progress", "Recovery operation ongoing");

        private final String displayName;
        private final String description;

        RecoveryStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class RecoveryAction {

        private final String actionType;
        private final Long blockNumber;
        private final String description;
        private final boolean success;
        private final LocalDateTime timestamp;
        private final Duration duration;

        public RecoveryAction(
            String actionType,
            Long blockNumber,
            String description,
            boolean success,
            Duration duration
        ) {
            this.actionType = actionType;
            this.blockNumber = blockNumber;
            this.description = description;
            this.success = success;
            this.timestamp = LocalDateTime.now();
            this.duration = duration;
        }

        // Getters
        public String getActionType() {
            return actionType;
        }

        public Long getBlockNumber() {
            return blockNumber;
        }

        public String getDescription() {
            return description;
        }

        public boolean isSuccess() {
            return success;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public Duration getDuration() {
            return duration;
        }
    }

    public static class RecoveryStatistics {

        private long totalBlocksAnalyzed = 0;
        private long corruptedBlocksFound = 0;
        private long blocksRepaired = 0;
        private long blocksRolledBack = 0;
        private long orphanBlocksRemoved = 0;
        private long dataBytesRecovered = 0;
        private Duration totalRecoveryTime = Duration.ZERO;

        // Builder methods
        public RecoveryStatistics withBlocksAnalyzed(long count) {
            this.totalBlocksAnalyzed = count;
            return this;
        }

        public RecoveryStatistics withCorruptedBlocks(long count) {
            this.corruptedBlocksFound = count;
            return this;
        }

        public RecoveryStatistics withBlocksRepaired(long count) {
            this.blocksRepaired = count;
            return this;
        }

        public RecoveryStatistics withBlocksRolledBack(long count) {
            this.blocksRolledBack = count;
            return this;
        }

        public RecoveryStatistics withOrphanBlocksRemoved(long count) {
            this.orphanBlocksRemoved = count;
            return this;
        }

        public RecoveryStatistics withDataRecovered(long bytes) {
            this.dataBytesRecovered = bytes;
            return this;
        }

        public RecoveryStatistics withTotalTime(Duration duration) {
            this.totalRecoveryTime = duration;
            return this;
        }

        // Getters
        public long getTotalBlocksAnalyzed() {
            return totalBlocksAnalyzed;
        }

        public long getCorruptedBlocksFound() {
            return corruptedBlocksFound;
        }

        public long getBlocksRepaired() {
            return blocksRepaired;
        }

        public long getBlocksRolledBack() {
            return blocksRolledBack;
        }

        public long getOrphanBlocksRemoved() {
            return orphanBlocksRemoved;
        }

        public long getDataBytesRecovered() {
            return dataBytesRecovered;
        }

        public Duration getTotalRecoveryTime() {
            return totalRecoveryTime;
        }

        public double getSuccessRate() {
            if (corruptedBlocksFound == 0) return 100.0;
            return (blocksRepaired * 100.0) / corruptedBlocksFound;
        }
    }

    private final String recoveryId;
    private final RecoveryStatus status;
    private final String message;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<RecoveryAction> actions;
    private final RecoveryStatistics statistics;
    private final List<String> warnings;
    private final List<String> recommendations;
    private final Map<String, Object> metadata;

    public ChainRecoveryResult(
        String recoveryId,
        RecoveryStatus status,
        String message
    ) {
        this.recoveryId = recoveryId;
        this.status = status;
        this.message = message;
        this.startTime = LocalDateTime.now();
        this.endTime = LocalDateTime.now();
        this.actions = new ArrayList<>();
        this.statistics = new RecoveryStatistics();
        this.warnings = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public ChainRecoveryResult(
        String recoveryId,
        RecoveryStatus status,
        String message,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<RecoveryAction> actions,
        RecoveryStatistics statistics
    ) {
        this.recoveryId = recoveryId;
        this.status = status;
        this.message = message;
        this.startTime = startTime;
        this.endTime = endTime;
        this.actions = new ArrayList<>(actions);
        this.statistics = statistics;
        this.warnings = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public ChainRecoveryResult addAction(RecoveryAction action) {
        this.actions.add(action);
        return this;
    }

    public ChainRecoveryResult addWarning(String warning) {
        this.warnings.add(warning);
        return this;
    }

    public ChainRecoveryResult addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
        return this;
    }

    public ChainRecoveryResult addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    // Analysis methods
    public List<RecoveryAction> getFailedActions() {
        return actions
            .stream()
            .filter(action -> action != null && !action.isSuccess())
            .collect(Collectors.toList());
    }

    public Map<String, List<RecoveryAction>> groupActionsByType() {
        Map<String, List<RecoveryAction>> grouped = new HashMap<>();
        for (RecoveryAction action : actions) {
            if (action != null) {
                grouped
                    .computeIfAbsent(action.getActionType(), k ->
                        new ArrayList<>()
                    )
                    .add(action);
            }
        }
        return grouped;
    }

    public Duration getTotalDuration() {
        return Duration.between(startTime, endTime);
    }

    // Getters
    public String getRecoveryId() {
        return recoveryId;
    }

    public RecoveryStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<RecoveryAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public RecoveryStatistics getStatistics() {
        return statistics;
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<String> getRecommendations() {
        return Collections.unmodifiableList(recommendations);
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔧 Chain Recovery Report\n");
        sb.append("========================\n\n");

        sb.append(String.format("📋 Recovery ID: %s\n", recoveryId));
        sb.append(String.format("📊 Status: %s\n", status.getDisplayName()));
        sb.append(String.format("💬 Message: %s\n", message));
        sb.append(String.format("⏱️ Duration: %s\n", getTotalDuration()));
        sb.append(String.format("📅 Completed: %s\n\n", endTime));

        // Statistics
        sb.append("📈 Recovery Statistics:\n");
        sb.append(
            String.format(
                "  - Blocks Analyzed: %d\n",
                statistics.getTotalBlocksAnalyzed()
            )
        );
        sb.append(
            String.format(
                "  - Corrupted Found: %d\n",
                statistics.getCorruptedBlocksFound()
            )
        );
        sb.append(
            String.format(
                "  - Blocks Repaired: %d\n",
                statistics.getBlocksRepaired()
            )
        );
        sb.append(
            String.format(
                "  - Blocks Rolled Back: %d\n",
                statistics.getBlocksRolledBack()
            )
        );
        sb.append(
            String.format(
                "  - Orphans Removed: %d\n",
                statistics.getOrphanBlocksRemoved()
            )
        );
        sb.append(
            String.format(
                "  - Data Recovered: %.2f MB\n",
                statistics.getDataBytesRecovered() / (1024.0 * 1024.0)
            )
        );
        sb.append(
            String.format(
                "  - Success Rate: %.2f%%\n\n",
                statistics.getSuccessRate()
            )
        );

        // Actions summary
        if (!actions.isEmpty()) {
            sb.append("🔄 Recovery Actions:\n");
            Map<String, List<RecoveryAction>> groupedActions =
                groupActionsByType();
            for (Map.Entry<
                String,
                List<RecoveryAction>
            > entry : groupedActions.entrySet()) {
                long successful = entry
                    .getValue()
                    .stream()
                    .mapToLong(action -> action.isSuccess() ? 1 : 0)
                    .sum();
                sb.append(
                    String.format(
                        "  - %s: %d/%d successful\n",
                        entry.getKey(),
                        successful,
                        entry.getValue().size()
                    )
                );
            }
            sb.append("\n");
        }

        // Warnings
        if (!warnings.isEmpty()) {
            sb.append("⚠️ Warnings:\n");
            warnings.forEach(warning ->
                sb.append("  - ").append(warning).append("\n")
            );
            sb.append("\n");
        }

        // Recommendations
        if (!recommendations.isEmpty()) {
            sb.append("💡 Recommendations:\n");
            recommendations.forEach(rec ->
                sb.append("  - ").append(rec).append("\n")
            );
        }

        return sb.toString();
    }
}
