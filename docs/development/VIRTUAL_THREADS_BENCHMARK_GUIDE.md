# Virtual Threads Benchmark Guide

**Project:** Private Blockchain (Java 25)
**Created:** 2025-12-26
**Java Version:** Java 25 LTS
**Purpose:** Performance analysis and monitoring of Java 25 Virtual Threads implementation

---

## 📋 Overview

This guide explains how to benchmark and monitor the Virtual Threads implementation in the Private Blockchain application. After migrating from platform threads to virtual threads, these tools help measure the actual performance improvements.

**Tools included:**
- ✅ **VirtualThreadsBenchmark** - Comprehensive performance benchmark suite
- ✅ **ThreadDumpAnalyzer** - Real-time thread pattern analysis
- ✅ **Performance Dashboard** - Live monitoring (existing tool)

---

## 🚀 Quick Start

### Run Complete Benchmark Suite

```bash
# Execute all benchmarks (5-10 minutes)
./tools/run_virtual_threads_benchmark.zsh
```

### Analyze Thread Patterns

```bash
# Analyze current thread dump
./tools/run_thread_dump_analyzer.zsh
```

### Real-Time Monitoring

```bash
# Launch performance dashboard
./tools/run_performance_dashboard.zsh
```

---

## 📊 Benchmark Suite Details

### 1. Concurrent Block Creation Benchmark

**Purpose**: Measures throughput of creating blocks concurrently

**Test Scenarios**:
- 10, 50, 100, 500, 1000 concurrent threads
- 100 operations per thread
- Measures: throughput (ops/sec), latency, success rate

**Expected Improvements with Virtual Threads**:
- **10x-50x** higher throughput at high concurrency (500-1000 threads)
- **10x-20x** lower latency under load
- Near-linear scaling with increased concurrency

**Example Output**:
```
📦 BENCHMARK 1: Concurrent Block Creation
----------------------------------------------------------------
   Concurrency: 10 | Avg: 245.20ms | Min: 232ms | Max: 268ms | StdDev: 11.25ms
   Concurrency: 50 | Avg: 512.45ms | Min: 489ms | Max: 547ms | StdDev: 18.30ms
   Concurrency: 100 | Avg: 823.10ms | Min: 801ms | Max: 865ms | StdDev: 22.15ms
   Concurrency: 500 | Avg: 2145.30ms | Min: 2098ms | Max: 2205ms | StdDev: 35.40ms
   Concurrency: 1000 | Avg: 3892.50ms | Min: 3812ms | Max: 3998ms | StdDev: 62.30ms
```

---

### 2. Concurrent Search Operations Benchmark

**Purpose**: Measures search performance with virtual threads

**Setup**:
- 1000 test blocks pre-indexed
- Multiple search queries executed concurrently

**Test Scenarios**:
- 10, 50, 100, 500, 1000 concurrent search threads
- 10 searches per thread
- Measures: search throughput, result accuracy

**Expected Improvements**:
- **25x** faster concurrent searches
- Better cache utilization
- Reduced context switching overhead

**Example Output**:
```
🔍 BENCHMARK 2: Concurrent Search Operations
----------------------------------------------------------------
   Total searches: 100 | Total results: 523
   Duration: 145 ms
   Search throughput: 689.66 searches/sec
```

---

### 3. Concurrent Indexing Operations Benchmark

**Purpose**: Measures indexing performance

**Test Scenarios**:
- 10, 50, 100 blocks indexed concurrently
- Full-text indexing with SearchFrameworkEngine

**Expected Improvements**:
- **100x** faster bulk indexing
- Better CPU utilization during I/O waits

**Example Output**:
```
📇 BENCHMARK 3: Concurrent Indexing Operations
----------------------------------------------------------------
   Indexed 100 blocks in 234 ms
   Indexing rate: 427.35 blocks/sec
```

---

### 4. Memory Usage Analysis

**Purpose**: Measures memory efficiency of virtual threads vs platform threads

**Test Scenarios**:
- 100, 1000, 10000 concurrent threads
- Measures memory per thread

**Expected Improvements**:
- **2500x** less memory per thread (~400 bytes vs 1-2 MB)
- Support for millions of concurrent operations
- Reduced GC pressure

**Example Output**:
```
💾 BENCHMARK 4: Memory Usage Analysis
----------------------------------------------------------------
   Baseline memory: 45 MB
   100 virtual threads: 4 KB per thread
   1000 virtual threads: 2 KB per thread
   10000 virtual threads: 1 KB per thread
```

---

## 🔍 Thread Dump Analyzer

### What It Analyzes

1. **Thread Distribution**
   - Virtual threads vs Platform threads
   - Carrier thread count
   - Thread type categorization

2. **Thread States**
   - RUNNABLE, WAITING, BLOCKED distribution
   - Carrier thread utilization
   - Pinned virtual threads detection

3. **Blocking Patterns**
   - Contended locks identification
   - Deadlock detection
   - Performance bottlenecks

### Example Output

```
🔍 Thread Dump Analyzer - Java 25 Virtual Threads
================================================================================

📊 Thread Summary:
   Total Threads: 156
   Virtual Threads: 142 (91.0%)
   Platform Threads: 14 (9.0%)
   Carrier Threads: 8
   CPU Cores: 8

📂 Thread Type Distribution:
   VirtualThread: 142
   CarrierThread: 8
   DatabasePool: 3
   VMThread: 2
   GarbageCollector: 1

🔄 Thread State Distribution:
   RUNNABLE: 98
   WAITING: 45
   TIMED_WAITING: 13

🚀 Carrier Thread Analysis:
   Total Carrier Threads: 8
   RUNNABLE: 7
   WAITING: 1
   Estimated Utilization: 7/8 cores (87.5%)

✅ No blocked threads detected

🌟 Virtual Threads Detail:
   RUNNABLE: 89
   WAITING: 42
   TIMED_WAITING: 11
```

---

## 📈 Performance Comparison: Platform vs Virtual Threads

### Theoretical Benefits

| Metric | Platform Threads | Virtual Threads | Improvement |
|--------|------------------|-----------------|-------------|
| **Memory per thread** | 1-2 MB | ~400 bytes | **2500x** |
| **Max concurrent** | ~1,000 | ~1,000,000 | **1000x** |
| **Thread creation** | ~1ms | ~1μs | **1000x** |
| **Context switch** | High (OS) | Low (JVM) | **10x** |
| **Blocking I/O cost** | Expensive | Nearly free | **∞** |

### Real-World Expected Improvements

Based on the implementation:

| Operation | Expected Improvement |
|-----------|---------------------|
| **Bulk Indexing** (10K blocks) | 100x faster |
| **Concurrent Searches** (1K users) | 25x faster |
| **File Cleanup** (50K files) | 1000x faster |
| **Block Creation** (1K concurrent) | 24x faster |
| **GC Pause Times** | 20x-200x better |

---

## 🎯 How to Interpret Results

### Good Performance Indicators

✅ **High Virtual Thread Count**: 80%+ of total threads should be virtual
✅ **High Carrier Utilization**: 70-90% of cores actively used
✅ **Low Blocked Count**: < 5% of virtual threads blocked
✅ **Low Memory Per Thread**: < 5 KB per virtual thread
✅ **High Throughput**: Scales linearly with concurrency

### Performance Red Flags

❌ **Many Blocked Virtual Threads**: Indicates thread pinning (synchronized blocks)
❌ **Low Carrier Utilization**: < 50% suggests underutilization
❌ **High Memory Per Thread**: > 10 KB suggests leaks
❌ **Non-Linear Scaling**: Performance doesn't improve with more threads

---

## 🔧 Troubleshooting

### Virtual Threads Not Detected

**Symptom**: ThreadDumpAnalyzer shows 0 virtual threads

**Causes**:
1. Using Java < 25
2. Code still using platform thread pools

**Solution**:
```bash
# Verify Java version
java -version  # Should show Java 25

# Check for old thread pools
grep -r "Executors.newFixedThreadPool\|newCachedThreadPool" src/
```

---

### Poor Performance at High Concurrency

**Symptom**: Benchmark shows worse performance with 500+ threads

**Causes**:
1. Thread pinning (synchronized blocks)
2. CPU-bound operations (not I/O-bound)
3. Shared lock contention

**Solution**:
```bash
# Analyze thread dumps under load
./tools/run_thread_dump_analyzer.zsh

# Look for:
# - High BLOCKED virtual thread count
# - Contended locks
# - Low carrier utilization
```

---

### High Memory Usage

**Symptom**: Memory per thread > 10 KB

**Causes**:
1. ThreadLocal variables in virtual threads
2. Large stack frames
3. Memory leaks

**Solution**:
- Review ThreadLocal usage
- Replace ThreadLocal with ScopedValue (Java 25+)
- Analyze heap dumps

---

## 📋 Integration with CI/CD

### Automated Benchmarking

```bash
# Add to CI pipeline
#!/bin/bash
# Run benchmarks and fail if performance regresses

./tools/run_virtual_threads_benchmark.zsh > benchmark_results.txt

# Parse results and compare with baseline
# Fail build if throughput decreases > 10%
```

### Continuous Monitoring

```bash
# Production monitoring script
#!/bin/bash
# Capture thread dumps periodically

while true; do
    ./tools/run_thread_dump_analyzer.zsh >> /var/log/thread_analysis.log
    sleep 300  # Every 5 minutes
done
```

---

## 📚 Related Documentation

- **[VIRTUAL_THREADS_INVESTIGATION_REPORT.md](VIRTUAL_THREADS_INVESTIGATION_REPORT.md)** - Complete implementation details
- **[JAVA_21_25_FEATURES_OPTIMIZATION_REPORT.md](JAVA_21_25_FEATURES_OPTIMIZATION_REPORT.md)** - Full Java 25 optimization guide
- **[PERFORMANCE_METRICS_GUIDE.md](../monitoring/PERFORMANCE_METRICS_GUIDE.md)** - General performance monitoring

---

## 🎯 Next Steps

After running benchmarks:

1. ✅ Document actual performance improvements
2. ✅ Compare with theoretical estimates
3. ✅ Identify bottlenecks and optimization opportunities
4. ✅ Set up continuous monitoring in production
5. ✅ Create performance regression tests

---

**Last Updated**: 2025-12-26
**Project Version**: 1.0.6
**Status**: Ready for production benchmarking
