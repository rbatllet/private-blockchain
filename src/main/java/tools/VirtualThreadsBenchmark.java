package tools;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive benchmark suite for Java 25 Virtual Threads performance analysis.
 *
 * Measures:
 * - Concurrent block creation throughput
 * - Concurrent search operations performance
 * - Memory usage patterns
 * - Thread creation overhead
 * - Context switching efficiency
 *
 * @since 1.0.6
 */
public class VirtualThreadsBenchmark {

    // Benchmark configuration
    private static final int WARMUP_ITERATIONS = 5;
    private static final int BENCHMARK_ITERATIONS = 10;

    // Test scenarios
    private static final int[] CONCURRENCY_LEVELS = {10, 50, 100, 500, 1000};
    private static final int OPERATIONS_PER_THREAD = 100;

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Virtual Threads Benchmark Suite - Java 25");
        System.out.println("=".repeat(80));

        printSystemInfo();

        // Run benchmark suites
        runBlockCreationBenchmark();
        runSearchOperationsBenchmark();
        runIndexingBenchmark();
        runMemoryBenchmark();

        System.out.println("✅ Benchmark suite completed");
    }

    private static void printSystemInfo() {
        System.out.println("📊 System Information:");
        System.out.printf("   Java Version: %s%n", System.getProperty("java.version"));
        System.out.printf("   Java Vendor: %s%n", System.getProperty("java.vendor"));
        System.out.printf("   OS: %s %s%n", System.getProperty("os.name"), System.getProperty("os.version"));
        System.out.printf("   CPU Cores: %d%n", Runtime.getRuntime().availableProcessors());
        System.out.printf("   Max Memory: %d MB%n", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        System.out.printf("   Available Memory: %d MB%n", Runtime.getRuntime().freeMemory() / 1024 / 1024);
        System.out.println();
    }

    /**
     * Benchmark 1: Concurrent Block Creation
     * Measures throughput of creating blocks concurrently with virtual threads
     */
    private static void runBlockCreationBenchmark() throws Exception {
        System.out.println("📦 BENCHMARK 1: Concurrent Block Creation");
        System.out.println("-".repeat(80));

        for (int concurrency : CONCURRENCY_LEVELS) {
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                benchmarkBlockCreation(concurrency, false);
            }

            // Actual benchmark
            List<Long> timings = new ArrayList<>();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long duration = benchmarkBlockCreation(concurrency, false);
                timings.add(duration);
            }

            printBenchmarkResults("Block Creation", concurrency, timings);
        }

        System.out.println();
    }

    private static long benchmarkBlockCreation(int concurrency, boolean verbose) throws Exception {
        Blockchain blockchain = new Blockchain();

        // Clean database before each benchmark
        blockchain.clearAndReinitialize();

        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());

        // Bootstrap admin
        blockchain.createBootstrapAdmin(publicKey, "Benchmark_Admin");

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(concurrency);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < concurrency; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String data = "Benchmark block " + taskId + "-" + j;
                        boolean success = blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic());
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    if (verbose) {
                        System.err.printf("Error in block creation: %s%n", e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        if (verbose) {
            System.out.printf("   Concurrency: %d threads%n", concurrency);
            System.out.printf("   Total operations: %d%n", concurrency * OPERATIONS_PER_THREAD);
            System.out.printf("   Successful: %d%n", successCount.get());
            System.out.printf("   Failed: %d%n", failureCount.get());
            System.out.printf("   Duration: %d ms%n", durationMs);
            System.out.printf("   Throughput: %.2f ops/sec%n", (double)(successCount.get() * 1000) / durationMs);
        }

        return durationMs;
    }

    /**
     * Benchmark 2: Concurrent Search Operations
     * Measures search performance with virtual threads
     */
    private static void runSearchOperationsBenchmark() throws Exception {
        System.out.println("🔍 BENCHMARK 2: Concurrent Search Operations");
        System.out.println("-".repeat(80));

        // Setup: Create blockchain with test data
        Blockchain blockchain = new Blockchain();

        // Clean database before benchmark
        blockchain.clearAndReinitialize();

        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKey, "Search_Admin");

        // Add test blocks
        System.out.println("   Setting up test blockchain with 1000 blocks...");
        for (int i = 0; i < 1000; i++) {
            String data = "Test data block " + i + " with searchable content: keyword" + (i % 10);
            blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic());
        }

        SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();
        searchEngine.indexBlockchain(blockchain, null, keyPair.getPrivate());

        for (int concurrency : CONCURRENCY_LEVELS) {
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                benchmarkSearchOperations(searchEngine, concurrency, false);
            }

            // Actual benchmark
            List<Long> timings = new ArrayList<>();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long duration = benchmarkSearchOperations(searchEngine, concurrency, false);
                timings.add(duration);
            }

            printBenchmarkResults("Search Operations", concurrency, timings);
        }

        searchEngine.shutdown();
        System.out.println();
    }

    private static long benchmarkSearchOperations(SearchFrameworkEngine searchEngine,
                                                   int concurrency, boolean verbose) throws Exception {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(concurrency);
        AtomicInteger searchCount = new AtomicInteger(0);
        AtomicLong totalResults = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < concurrency; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        String query = "keyword" + (taskId % 10);
                        SearchFrameworkEngine.SearchResult result = searchEngine.searchPublicOnly(query, 10);
                        searchCount.incrementAndGet();
                        totalResults.addAndGet(result.getResultCount());
                    }
                } catch (Exception e) {
                    if (verbose) {
                        System.err.printf("Search error: %s%n", e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        if (verbose) {
            System.out.printf("   Total searches: %d%n", searchCount.get());
            System.out.printf("   Total results: %d%n", totalResults.get());
            System.out.printf("   Duration: {} ms%n", durationMs);
            System.out.printf("   Search throughput: %.2f searches/sec%n", (double)(searchCount.get() * 1000) / durationMs);
        }

        return durationMs;
    }

    /**
     * Benchmark 3: Concurrent Indexing Operations
     * Measures indexing performance with virtual threads
     */
    private static void runIndexingBenchmark() throws Exception {
        System.out.println("📇 BENCHMARK 3: Concurrent Indexing Operations");
        System.out.println("-".repeat(80));

        for (int concurrency : new int[]{10, 50, 100}) {
            List<Long> timings = new ArrayList<>();

            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long duration = benchmarkIndexing(concurrency, false);
                timings.add(duration);
            }

            printBenchmarkResults("Indexing Operations", concurrency, timings);
        }

        System.out.println();
    }

    private static long benchmarkIndexing(int blockCount, boolean verbose) throws Exception {
        Blockchain blockchain = new Blockchain();

        // Clean database before benchmark
        blockchain.clearAndReinitialize();

        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKey, "Index_Admin");

        // Create blocks sequentially
        for (int i = 0; i < blockCount; i++) {
            blockchain.addBlock("Indexing test block " + i, keyPair.getPrivate(), keyPair.getPublic());
        }

        SearchFrameworkEngine searchEngine = new SearchFrameworkEngine();

        long startTime = System.nanoTime();
        searchEngine.indexBlockchain(blockchain, null, keyPair.getPrivate());
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        searchEngine.shutdown();

        if (verbose) {
            System.out.printf("   Indexed {} blocks in {} ms%n", blockCount, durationMs);
            System.out.printf("   Indexing rate: %.2f blocks/sec%n", (double)(blockCount * 1000) / durationMs);
        }

        return durationMs;
    }

    /**
     * Benchmark 4: Memory Usage Analysis
     * Measures memory efficiency of virtual threads
     */
    private static void runMemoryBenchmark() throws Exception {
        System.out.println("💾 BENCHMARK 4: Memory Usage Analysis");
        System.out.println("-".repeat(80));

        Runtime runtime = Runtime.getRuntime();

        // Baseline memory
        System.gc();
        Thread.sleep(1000);
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

        System.out.printf("   Baseline memory: {} MB%n", baselineMemory / 1024 / 1024);

        // Create many virtual threads and measure memory
        for (int threadCount : new int[]{100, 1000, 10000}) {
            System.gc();
            Thread.sleep(500);

            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            Thread.sleep(50); // Let threads start

            long duringMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryPerThread = (duringMemory - beforeMemory) / threadCount;

            latch.await();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            System.out.printf("   %d virtual threads: %d KB per thread%n", threadCount, memoryPerThread / 1024);
        }

        System.out.println();
    }

    /**
     * Print benchmark results with statistics
     */
    private static void printBenchmarkResults(String benchmarkName, int concurrency,
                                             List<Long> timings) {
        double avg = timings.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = timings.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = timings.stream().mapToLong(Long::longValue).max().orElse(0);

        // Calculate standard deviation
        double variance = timings.stream()
            .mapToDouble(t -> Math.pow(t - avg, 2))
            .average()
            .orElse(0);
        double stdDev = Math.sqrt(variance);

        System.out.printf("   Concurrency: %d | Avg: %.2fms | Min: %dms | Max: %dms | StdDev: %.2fms%n", concurrency, avg, min, max, stdDev);
    }
}
