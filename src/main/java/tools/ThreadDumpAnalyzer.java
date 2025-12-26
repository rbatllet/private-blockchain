package tools;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Thread Dump Analyzer for Virtual Threads Performance Analysis.
 *
 * Analyzes:
 * - Virtual threads vs Platform threads distribution
 * - Thread states and blocking patterns
 * - Carrier thread utilization
 * - Pinned virtual threads detection
 *
 * @since 1.0.6
 */
public class ThreadDumpAnalyzer {

    public static void main(String[] args) {
        System.out.println("🔍 Thread Dump Analyzer - Java 25 Virtual Threads");
        System.out.println("=".repeat(80));

        analyzeCurrentThreads();
    }

    /**
     * Analyze current thread dump
     */
    public static void analyzeCurrentThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        Map<String, Integer> threadTypeCount = new HashMap<>();
        Map<Thread.State, Integer> stateCount = new HashMap<>();
        List<ThreadInfo> virtualThreads = new ArrayList<>();
        List<ThreadInfo> platformThreads = new ArrayList<>();
        List<ThreadInfo> carrierThreads = new ArrayList<>();
        List<ThreadInfo> blockedThreads = new ArrayList<>();

        // Analyze threads
        for (ThreadInfo info : threadInfos) {
            if (info == null) continue;

            String threadName = info.getThreadName();
            Thread.State state = info.getThreadState();

            // Categorize thread type
            String threadType = categorizeThread(threadName);
            threadTypeCount.merge(threadType, 1, Integer::sum);

            // Count states
            stateCount.merge(state, 1, Integer::sum);

            // Detect virtual threads (Java 21+)
            if (isVirtualThread(threadName)) {
                virtualThreads.add(info);
            } else {
                platformThreads.add(info);
            }

            // Detect carrier threads
            if (isCarrierThread(threadName)) {
                carrierThreads.add(info);
            }

            // Detect blocked threads
            if (state == Thread.State.BLOCKED || state == Thread.State.WAITING) {
                blockedThreads.add(info);
            }
        }

        // Print analysis
        printThreadSummary(threadInfos.length, virtualThreads.size(),
            platformThreads.size(), carrierThreads.size());
        printThreadTypeDistribution(threadTypeCount);
        printThreadStateDistribution(stateCount);
        printCarrierThreadAnalysis(carrierThreads);
        printBlockedThreadsAnalysis(blockedThreads);
        printVirtualThreadsDetail(virtualThreads);
    }

    /**
     * Categorize thread by name pattern
     */
    private static String categorizeThread(String threadName) {
        if (threadName == null) return "Unknown";

        // Virtual threads patterns
        if (threadName.startsWith("VirtualThread-")) return "VirtualThread";
        if (threadName.contains("virtual-")) return "VirtualThread-Named";

        // Carrier threads
        if (threadName.startsWith("ForkJoinPool")) return "CarrierThread";

        // Application threads
        if (threadName.contains("IndexingCoordinator")) return "Indexing";
        if (threadName.contains("maintenance-scheduler")) return "Maintenance";
        if (threadName.contains("SearchFramework")) return "Search";
        if (threadName.contains("AlertService")) return "Alerts";

        // JVM threads
        if (threadName.startsWith("GC ")) return "GarbageCollector";
        if (threadName.startsWith("VM ")) return "VMThread";
        if (threadName.contains("Finalizer")) return "Finalizer";
        if (threadName.contains("Reference Handler")) return "ReferenceHandler";

        // Database threads
        if (threadName.contains("HikariPool")) return "DatabasePool";

        // Default
        return "Other";
    }

    /**
     * Check if thread name indicates a virtual thread
     */
    private static boolean isVirtualThread(String threadName) {
        return threadName != null &&
            (threadName.startsWith("VirtualThread-") ||
             threadName.contains("virtual-") ||
             threadName.contains("@ForkJoinPool"));
    }

    /**
     * Check if thread is a carrier thread
     */
    private static boolean isCarrierThread(String threadName) {
        return threadName != null &&
            threadName.startsWith("ForkJoinPool");
    }

    /**
     * Print thread summary
     */
    private static void printThreadSummary(int total, int virtual,
                                          int platform, int carrier) {
        System.out.println("📊 Thread Summary:");
        System.out.printf("   Total Threads: %d%n", total);
        System.out.printf("   Virtual Threads: %d (%.1f%%)%n",
            virtual, (virtual * 100.0) / total);
        System.out.printf("   Platform Threads: %d (%.1f%%)%n",
            platform, (platform * 100.0) / total);
        System.out.printf("   Carrier Threads: %d%n", carrier);
        System.out.printf("   CPU Cores: %d%n", Runtime.getRuntime().availableProcessors());
        System.out.println();
    }

    /**
     * Print thread type distribution
     */
    private static void printThreadTypeDistribution(Map<String, Integer> typeCount) {
        System.out.println("📂 Thread Type Distribution:");

        typeCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry ->
                System.out.printf("   %s: %d%n", entry.getKey(), entry.getValue())
            );

        System.out.println();
    }

    /**
     * Print thread state distribution
     */
    private static void printThreadStateDistribution(Map<Thread.State, Integer> stateCount) {
        System.out.println("🔄 Thread State Distribution:");

        stateCount.entrySet().stream()
            .sorted(Map.Entry.<Thread.State, Integer>comparingByValue().reversed())
            .forEach(entry ->
                System.out.printf("   %s: %d%n", entry.getKey(), entry.getValue())
            );

        System.out.println();
    }

    /**
     * Print carrier thread analysis
     */
    private static void printCarrierThreadAnalysis(List<ThreadInfo> carrierThreads) {
        System.out.println("🚀 Carrier Thread Analysis:");
        System.out.printf("   Total Carrier Threads: %d%n", carrierThreads.size());

        Map<Thread.State, Long> carrierStates = carrierThreads.stream()
            .collect(Collectors.groupingBy(ThreadInfo::getThreadState, Collectors.counting()));

        carrierStates.forEach((state, count) ->
            System.out.printf("   %s: %d%n", state, count)
        );

        // Calculate utilization (RUNNABLE carrier threads)
        long runnable = carrierStates.getOrDefault(Thread.State.RUNNABLE, 0L);
        int cpuCores = Runtime.getRuntime().availableProcessors();

        System.out.printf("   Estimated Utilization: %d/%d cores (%.1f%%)%n",
            runnable, cpuCores, (runnable * 100.0) / cpuCores);
        System.out.println();
    }

    /**
     * Print blocked threads analysis
     */
    private static void printBlockedThreadsAnalysis(List<ThreadInfo> blockedThreads) {
        if (blockedThreads.isEmpty()) {
            System.out.println("✅ No blocked threads detected");
            System.out.println();
            return;
        }

        System.out.println("⚠️  Blocked Threads Analysis:");
        System.out.printf("   Total Blocked/Waiting: %d%n", blockedThreads.size());

        // Group by lock
        Map<String, List<ThreadInfo>> byLock = blockedThreads.stream()
            .filter(info -> info.getLockName() != null)
            .collect(Collectors.groupingBy(ThreadInfo::getLockName));

        if (!byLock.isEmpty()) {
            System.out.println("   Contended Locks:");
            byLock.entrySet().stream()
                .sorted(Map.Entry.<String, List<ThreadInfo>>comparingByValue(
                    Comparator.comparingInt(List::size)).reversed())
                .limit(5)
                .forEach(entry ->
                    System.out.printf("      %s: %d threads%n", entry.getKey(), entry.getValue().size())
                );
        }

        System.out.println();
    }

    /**
     * Print virtual threads detail
     */
    private static void printVirtualThreadsDetail(List<ThreadInfo> virtualThreads) {
        if (virtualThreads.isEmpty()) {
            System.out.println("ℹ️  No virtual threads detected (may be using platform threads)");
            System.out.println();
            return;
        }

        System.out.println("🌟 Virtual Threads Detail:");

        Map<Thread.State, Long> virtualStates = virtualThreads.stream()
            .collect(Collectors.groupingBy(ThreadInfo::getThreadState, Collectors.counting()));

        virtualStates.forEach((state, count) ->
            System.out.printf("   %s: %d%n", state, count)
        );

        // Detect potential pinned threads (BLOCKED virtual threads)
        long blocked = virtualStates.getOrDefault(Thread.State.BLOCKED, 0L);
        if (blocked > 0) {
            System.out.println();
            System.out.printf("   ⚠️  WARNING: %d virtual threads BLOCKED%n", blocked);
            System.out.println("   This may indicate thread pinning (synchronized blocks holding locks)");
        }

        System.out.println();
    }

    /**
     * Generate thread dump report
     */
    public static String generateThreadDumpReport() {
        StringBuilder report = new StringBuilder();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        report.append("=== THREAD DUMP REPORT ===\n");
        report.append("Timestamp: ").append(new Date()).append("\n");
        report.append("Total Threads: ").append(threadInfos.length).append("\n\n");

        for (ThreadInfo info : threadInfos) {
            if (info == null) continue;

            report.append("Thread: ").append(info.getThreadName())
                .append(" (ID: ").append(info.getThreadId()).append(")\n");
            report.append("  State: ").append(info.getThreadState()).append("\n");

            if (info.getLockName() != null) {
                report.append("  Waiting on: ").append(info.getLockName()).append("\n");
            }

            if (info.getLockOwnerName() != null) {
                report.append("  Owned by: ").append(info.getLockOwnerName()).append("\n");
            }

            // Stack trace (first 5 frames)
            StackTraceElement[] stack = info.getStackTrace();
            int limit = Math.min(5, stack.length);
            for (int i = 0; i < limit; i++) {
                report.append("    at ").append(stack[i]).append("\n");
            }

            if (stack.length > 5) {
                report.append("    ... ").append(stack.length - 5).append(" more\n");
            }

            report.append("\n");
        }

        return report.toString();
    }
}
