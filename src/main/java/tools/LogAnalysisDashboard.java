package tools;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Log Analysis Dashboard - Real-time performance monitoring
 * Analyzes blockchain logs to display live metrics
 */
public class LogAnalysisDashboard {
    
    private static final String LOGS_DIR = "logs";
    private static final int REFRESH_INTERVAL_MS = 1000;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile boolean running = true;
    
    // Metrics from log analysis
    private int totalBlocks = 0;
    private int successfulOperations = 0;
    private int errors = 0;
    private int warnings = 0;
    private double avgResponseTime = 0;
    private long memoryUsage = 0;
    private final List<String> recentAlerts = new ArrayList<>();
    private final Map<String, Integer> operationCounts = new HashMap<>();
    
    public void start() {
        printHeader();
        
        // Schedule periodic updates
        scheduler.scheduleAtFixedRate(this::updateDashboard, 0, REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        // Keep main thread alive
        try {
            while (running) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void printHeader() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 LOG ANALYSIS DASHBOARD - Real-time Blockchain Monitoring");
        System.out.println("=".repeat(80));
        System.out.println("Analyzing logs in: " + LOGS_DIR);
        System.out.println("Updates every second | Press Ctrl+C to stop\n");
    }
    
    private void updateDashboard() {
        try {
            // Analyze all log files
            analyzeLogs();
            
            // Clear screen (works on most terminals)
            System.out.print("\033[H\033[2J");
            System.out.flush();
            
            // Print dashboard
            printDashboard();
            
        } catch (Exception e) {
            System.err.println("Dashboard error: " + e.getMessage());
        }
    }
    
    private void analyzeLogs() throws IOException {
        // Reset counters
        totalBlocks = 0;
        successfulOperations = 0;
        errors = 0;
        warnings = 0;
        operationCounts.clear();
        recentAlerts.clear();
        
        // Analyze blockchain.log
        Path blockchainLog = Paths.get(LOGS_DIR, "blockchain.log");
        if (Files.exists(blockchainLog)) {
            analyzeBlockchainLog(blockchainLog);
        }
        
        // Also analyze test-app.log (where test output goes)
        Path testAppLog = Paths.get(LOGS_DIR, "test-app.log");
        if (Files.exists(testAppLog)) {
            analyzeBlockchainLog(testAppLog);
        }
        
        // Analyze performance-metrics.log
        Path perfLog = Paths.get(LOGS_DIR, "performance-metrics.log");
        if (Files.exists(perfLog)) {
            analyzePerformanceLog(perfLog);
        }
        
        // Analyze structured-alerts.log
        Path alertsLog = Paths.get(LOGS_DIR, "structured-alerts.log");
        if (Files.exists(alertsLog)) {
            analyzeAlertsLog(alertsLog);
        }
    }
    
    private void analyzeBlockchainLog(Path logFile) throws IOException {
        List<String> lines = Files.readAllLines(logFile);
        List<Double> responseTimes = new ArrayList<>();
        
        for (String line : lines) {
            // Count blocks
            if (line.contains("Block #") && line.contains("added successfully")) {
                totalBlocks++;
            }
            
            // Count operations
            if (line.contains("✅")) {
                successfulOperations++;
            }
            
            // Count errors and warnings (but exclude DEBUG messages)
            if ((line.contains("ERROR") || line.contains("❌")) && !line.contains("DEBUG")) {
                errors++;
            }
            if ((line.contains("WARN") || line.contains("⚠️")) && !line.contains("DEBUG")) {
                warnings++;
            }
            
            // Extract operation types
            if (line.contains("BLOCK_SAVE") || line.contains("SEARCH") || 
                line.contains("VALIDATION") || line.contains("EXPORT")) {
                String operation = extractOperation(line);
                operationCounts.merge(operation, 1, Integer::sum);
            }
            
            // Extract duration and memory from AdvancedLoggingService logs
            if (line.contains("Duration: ") && line.contains("ms")) {
                try {
                    int start = line.indexOf("Duration: ") + 10;
                    int end = line.indexOf("ms", start);
                    if (start > 9 && end > start) {
                        String timeStr = line.substring(start, end).trim();
                        responseTimes.add(Double.parseDouble(timeStr));
                    }
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
            
            if (line.contains("Memory: ") && line.contains("MB")) {
                try {
                    int start = line.indexOf("Memory: ") + 8;
                    int end = line.indexOf("MB", start);
                    if (start > 7 && end > start) {
                        String memStr = line.substring(start, end).trim();
                        long mem = Long.parseLong(memStr);
                        if (mem > memoryUsage) {
                            memoryUsage = mem;
                        }
                    }
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }
        
        // Calculate average response time
        if (!responseTimes.isEmpty()) {
            avgResponseTime = responseTimes.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
        }
    }
    
    private void analyzePerformanceLog(Path logFile) throws IOException {
        // We don't need this method anymore since performance data is in blockchain.log
        // Keep it empty for now in case we need it later
    }
    
    private void analyzeAlertsLog(Path logFile) throws IOException {
        List<String> lines = Files.readAllLines(logFile);
        
        // Get last 5 alerts
        int start = Math.max(0, lines.size() - 5);
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("message")) {
                recentAlerts.add(extractAlertMessage(line));
            }
        }
    }
    
    private String extractOperation(String line) {
        if (line.contains("BLOCK_SAVE")) return "BLOCK_SAVE";
        if (line.contains("SEARCH")) return "SEARCH";
        if (line.contains("VALIDATION")) return "VALIDATION";
        if (line.contains("EXPORT")) return "EXPORT";
        return "OTHER";
    }
    
    private String extractAlertMessage(String line) {
        // Simple extraction - can be improved with JSON parsing
        int start = line.indexOf("\"message\":\"") + 11;
        int end = line.indexOf("\"", start);
        if (start > 10 && end > start) {
            return line.substring(start, end);
        }
        return line;
    }
    
    private void printDashboard() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 BLOCKCHAIN PERFORMANCE DASHBOARD - " + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("=".repeat(80) + "\n");
        
        // Core Metrics
        System.out.println("📈 CORE METRICS");
        System.out.println("─".repeat(40));
        System.out.printf("📦 Total Blocks:           %d%n", totalBlocks);
        System.out.printf("✅ Successful Operations:  %d%n", successfulOperations);
        System.out.printf("⏱️  Avg Response Time:      %.0f ms%n", avgResponseTime);
        System.out.printf("💾 Memory Usage:           %d MB%n", memoryUsage);
        System.out.printf("❌ Errors:                 %d%n", errors);
        System.out.printf("⚠️  Warnings:               %d%n", warnings);
        
        // Operation Breakdown
        System.out.println("\n⚙️  OPERATION BREAKDOWN");
        System.out.println("─".repeat(40));
        if (operationCounts.isEmpty()) {
            System.out.println("No operations recorded yet");
        } else {
            operationCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf("%-15s: %d%n", e.getKey(), e.getValue()));
        }
        
        // Recent Alerts
        System.out.println("\n🚨 RECENT ALERTS");
        System.out.println("─".repeat(40));
        if (recentAlerts.isEmpty()) {
            System.out.println("✅ No recent alerts");
        } else {
            recentAlerts.forEach(alert -> System.out.println("• " + alert));
        }
        
        // Health Status
        System.out.println("\n💚 HEALTH STATUS");
        System.out.println("─".repeat(40));
        String healthStatus = determineHealthStatus();
        System.out.println("Overall Status: " + healthStatus);
        
        // Performance Trend
        System.out.println("\n📊 PERFORMANCE INDICATORS");
        System.out.println("─".repeat(40));
        printPerformanceBar("Response Time", avgResponseTime, 1000, "ms");
        printPerformanceBar("Memory Usage", memoryUsage, 500, "MB");
        printPerformanceBar("Error Rate", (errors * 100.0) / Math.max(1, successfulOperations), 5, "%");
        
        System.out.println("\n" + "─".repeat(80));
        System.out.println("🔄 Refreshing... | 📁 Logs: " + LOGS_DIR + " | 🛑 Ctrl+C to stop");
    }
    
    private String determineHealthStatus() {
        if (errors > 10) return "❌ CRITICAL - High error rate";
        if (warnings > 20) return "⚠️  WARNING - Multiple warnings detected";
        if (avgResponseTime > 500) return "⚠️  WARNING - Slow response times";
        if (memoryUsage > 400) return "⚠️  WARNING - High memory usage";
        return "✅ HEALTHY - All systems operational";
    }
    
    private void printPerformanceBar(String metric, double value, double maxValue, String unit) {
        System.out.printf("%-15s: ", metric);
        
        int barLength = 20;
        int filled = (int)((value / maxValue) * barLength);
        filled = Math.min(filled, barLength);
        filled = Math.max(0, filled);
        
        // Color coding
        String color = "";
        if (value < maxValue * 0.6) {
            color = "\033[32m"; // Green
        } else if (value < maxValue * 0.8) {
            color = "\033[33m"; // Yellow
        } else {
            color = "\033[31m"; // Red
        }
        
        System.out.print(color);
        System.out.print("█".repeat(filled));
        System.out.print("░".repeat(barLength - filled));
        System.out.print("\033[0m"); // Reset color
        System.out.printf(" %.1f %s%n", value, unit);
    }
    
    private void shutdown() {
        System.out.println("\n\n🛑 Shutting down dashboard...");
        running = false;
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Save final report
        saveFinalReport();
        
        System.out.println("✅ Dashboard stopped. Final report saved to logs directory.");
    }
    
    private void saveFinalReport() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path reportPath = Paths.get(LOGS_DIR, "dashboard-report-" + timestamp + ".txt");
            
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(reportPath))) {
                writer.println("LOG ANALYSIS DASHBOARD - FINAL REPORT");
                writer.println("Generated: " + LocalDateTime.now());
                writer.println("=".repeat(50));
                writer.println();
                writer.printf("Total Blocks Processed: %d%n", totalBlocks);
                writer.printf("Successful Operations: %d%n", successfulOperations);
                writer.printf("Average Response Time: %.0f ms%n", avgResponseTime);
                writer.printf("Peak Memory Usage: %d MB%n", memoryUsage);
                writer.printf("Total Errors: %d%n", errors);
                writer.printf("Total Warnings: %d%n", warnings);
                writer.println();
                writer.println("Operation Summary:");
                operationCounts.forEach((op, count) -> 
                    writer.printf("  %s: %d%n", op, count));
            }
            
        } catch (IOException e) {
            System.err.println("Failed to save report: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        LogAnalysisDashboard dashboard = new LogAnalysisDashboard();
        dashboard.start();
    }
}