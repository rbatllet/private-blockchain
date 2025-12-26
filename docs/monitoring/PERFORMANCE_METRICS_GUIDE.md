# Performance Metrics System Guide

## Overview

The Performance Metrics System provides comprehensive monitoring and analysis of blockchain operations, including response times, memory usage, throughput, and system health indicators.

## Architecture

### Core Components

#### PerformanceMetricsService
- **Singleton service** for centralized metrics collection
- **Thread-safe** implementation using `ConcurrentHashMap` and atomic operations
- **Real-time monitoring** with configurable thresholds and alerts

#### LoggingManager Integration
- **Automatic metrics recording** for all blockchain operations
- **Seamless integration** with existing logging infrastructure
- **Performance correlation** between operations and system resources

## Features

### 📊 Response Time Monitoring
- Average, minimum, maximum response times per operation type
- 95th percentile calculations for performance analysis
- Automatic detection of slow operations (>5 seconds)

### 💾 Memory Usage Tracking
- Operation-specific memory consumption monitoring
- System-wide heap utilization tracking
- Memory threshold alerts (Warning: 512MB, Critical: 1GB)

### 🛡️ Memory Safety Constants

The system includes centralized memory safety limits to prevent out-of-memory errors and ensure reliable operation even with very large blockchains:

```java
import com.rbatllet.blockchain.config.MemorySafetyConstants;

// Batch operation limits
MemorySafetyConstants.MAX_BATCH_SIZE                    // 10,000 - Maximum items for batch operations
MemorySafetyConstants.DEFAULT_BATCH_SIZE                // 1,000 - Default batch size for streaming

// Search and export limits  
MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS        // 10,000 - Default search result limit
MemorySafetyConstants.SAFE_EXPORT_LIMIT                 // 100,000 - Warning threshold for exports
MemorySafetyConstants.MAX_EXPORT_LIMIT                  // 500,000 - Hard limit for exports

// Validation and rollback limits
MemorySafetyConstants.LARGE_ROLLBACK_THRESHOLD          // 100,000 - Warning threshold for rollbacks
MemorySafetyConstants.PROGRESS_REPORT_INTERVAL          // 5,000 - Progress logging interval
```

#### Performance Impact of Memory Limits

**Validation Operations:**
- `validateChainDetailed()`:
  - ⚠️ **100,000 blocks**: Logs warning about high memory usage
  - ❌ **500,000 blocks**: Throws `IllegalStateException` (use streaming instead)
  - 💡 **Recommendation**: Use `validateChainStreaming()` for chains >100K blocks

- `validateChainStreaming()`:
  - ✅ **Unlimited size**: Processes chain in batches (default: 1,000 blocks)
  - 🚀 **Constant memory**: No memory accumulation regardless of chain size
  - 📊 **Progress tracking**: Reports progress every 5,000 blocks

**Export Operations:**
- Small chains (<100K blocks): Use standard `exportChain()`
- Large chains (100K-500K blocks): Warning logged, consider streaming export
- Very large chains (>500K blocks): Must use batch processing

**Search Operations:**
- Default limit: 10,000 results (configurable)
- Prevents memory exhaustion from large result sets
- Use pagination for accessing more results

**Best Practices:**
```java
// Check chain size before validation
long blockCount = blockchain.getBlockCount();

if (blockCount > MemorySafetyConstants.SAFE_EXPORT_LIMIT) {
    // Use streaming validation for large chains
    ValidationSummary summary = blockchain.validateChainStreaming(
        batchResults -> {
            // Process each batch
            logger.info("Processed batch with {} results", batchResults.size());
        },
        MemorySafetyConstants.DEFAULT_BATCH_SIZE
    );
} else {
    // Safe to use detailed validation for smaller chains
    ChainValidationResult result = blockchain.validateChainDetailed();
}
```

### ⚡ Throughput Analysis
- Items processed per operation tracking
- Peak throughput identification
- Operation efficiency metrics

### 🚨 Alert System
- **WARNING**: Slow operations, high memory usage
- **CRITICAL**: Memory exhaustion, system degradation
- **INFO**: General performance notifications

### 🏥 System Health Scoring
- Dynamic health score calculation (0-100)
- Based on error rates, memory usage, and active alerts
- Real-time status indicators

## Usage

### Basic Integration

```java
// Get service instance
PerformanceMetricsService metrics = PerformanceMetricsService.getInstance();

// Record operation metrics
metrics.recordResponseTime("BLOCKCHAIN_SEARCH", 150L);
metrics.recordMemoryUsage("BLOCKCHAIN_SEARCH", 45L);
metrics.recordThroughput("BLOCKCHAIN_SEARCH", 25);
metrics.recordOperation("BLOCKCHAIN_SEARCH", true);
```

### Automatic Integration via LoggingManager

```java
// All blockchain operations automatically record metrics
String result = LoggingManager.logBlockchainOperation(
    "SEARCH", 
    "Advanced search operation",
    dataSize,
    () -> performSearchOperation()
);
```

### Getting Performance Reports

```java
// Comprehensive performance report
String report = metrics.getPerformanceReport();

// System health summary
String health = metrics.getSystemHealthSummary();

// Active alerts summary
String alerts = metrics.getAlertsSummary();
```

## Configuration

### Performance Thresholds

```java
// Configurable thresholds in PerformanceMetricsService
private static final long SLOW_OPERATION_THRESHOLD_MS = 5000;  // 5 seconds
private static final long MEMORY_WARNING_THRESHOLD_MB = 512;   // 512 MB
private static final long MEMORY_CRITICAL_THRESHOLD_MB = 1024; // 1 GB
```

### Logging Configuration

Performance metrics are logged to separate appenders:
- `logs/performance-metrics-development.log`
- `logs/performance-metrics-production.log`

## Metrics Types

### Response Time Metrics
- **Count**: Total number of operations
- **Average**: Mean response time
- **Min/Max**: Performance bounds
- **95th Percentile**: Performance reliability indicator

### Memory Usage Metrics
- **Average Usage**: Mean memory consumption per operation
- **Peak Usage**: Maximum memory usage recorded
- **Min/Max**: Memory usage bounds

### Throughput Metrics
- **Total Items**: Cumulative items processed
- **Average/Operation**: Mean throughput efficiency
- **Peak/Operation**: Maximum throughput achieved

### System Overview
- **Total Operations**: All recorded operations
- **Total Errors**: Failed operations count
- **Error Rate**: Failure percentage
- **Active Threads**: Current thread utilization
- **Memory Utilization**: Heap usage percentage

## Health Score Calculation

The system calculates a dynamic health score (0-100) based on:

- **Error Rate Impact**:
  - >10%: -30 points
  - >5%: -20 points
  - >1%: -10 points

- **Memory Utilization Impact**:
  - >90%: -25 points
  - >80%: -15 points
  - >70%: -10 points

- **Alert Impact**:
  - Critical alerts: -15 points each
  - Warning alerts: -5 points each
  - Slow operation alerts: -5 points each
  - Memory alerts: -8 points each

### Health Status Indicators
- 🟢 **EXCELLENT** (90-100): Optimal performance
- 🟡 **GOOD** (75-89): Good performance with minor issues
- 🟠 **FAIR** (50-74): Acceptable performance with concerns
- 🔴 **POOR** (25-49): Performance degradation detected
- 🚨 **CRITICAL** (0-24): Immediate attention required

## Thread Safety

### Concurrent Data Structures
- `ConcurrentHashMap` for metrics storage
- `CopyOnWriteArrayList` for alerts management
- `LongAdder` for atomic counters
- `AtomicLong` for operation tracking

### Lock Strategy
- **Read-write locks** only for report generation
- **Lock-free operations** for metrics recording
- **Atomic operations** for counters and flags

## Performance Impact

### Minimal Overhead
- **Concurrent collections** eliminate lock contention
- **Atomic operations** provide thread-safe counters
- **Lazy calculation** for expensive metrics
- **Efficient memory management** with bounded collections

### Scalability
- **Thread-safe singleton** supports high concurrency
- **Non-blocking metrics recording** prevents performance degradation
- **Configurable thresholds** adapt to system capacity

## Integration Examples

### Search Operations
```java
// Automatic metrics via LoggingManager
List<Block> results = LoggingManager.logSearchOperation(
    "ENCRYPTED_SEARCH",
    searchParams,
    () -> cryptoSearch.searchEncrypted(query)
);
```

### Blockchain Operations
```java
// Manual metrics recording
long startTime = System.currentTimeMillis();
try {
    Block result = blockchain.addBlock(data);
    long duration = System.currentTimeMillis() - startTime;
    
    metrics.recordResponseTime("ADD_BLOCK", duration);
    metrics.recordOperation("ADD_BLOCK", true);
    metrics.recordMemoryUsage("ADD_BLOCK", estimateMemoryUsage(data));
    
} catch (Exception e) {
    long duration = System.currentTimeMillis() - startTime;
    metrics.recordResponseTime("ADD_BLOCK", duration);
    metrics.recordOperation("ADD_BLOCK", false);
    throw e;
}
```

## Monitoring and Alerts

### Real-time Monitoring
```java
// Periodic health checks (Java 25 Virtual Threads)
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());
scheduler.scheduleAtFixedRate(() -> {
    String health = metrics.getSystemHealthSummary();
    logger.info("System Health: {}", health);
}, 0, 5, TimeUnit.MINUTES);
```

### Alert Handling
```java
// Custom alert handlers can be implemented
public void handlePerformanceAlert(PerformanceAlert alert) {
    switch (alert.getSeverity()) {
        case CRITICAL:
            notificationService.sendCriticalAlert(alert);
            break;
        case WARNING:
            notificationService.sendWarning(alert);
            break;
        case INFO:
            logger.info("Performance info: {}", alert.getMessage());
            break;
    }
}
```

## Testing

### Unit Tests
- Comprehensive test suite with 12 test scenarios
- Concurrent testing with 10 threads and 100 operations per thread
- Thread-safe metrics validation
- Performance threshold testing

### Integration Tests
- LoggingManager integration validation
- End-to-end metrics collection testing
- Real blockchain operation monitoring

## Best Practices

### 🎯 Efficient Usage
- Use automatic integration via `LoggingManager` when possible
- Record metrics at operation boundaries, not inside loops
- Batch similar operations for better throughput metrics

### 🔧 Configuration
- Adjust thresholds based on system capacity and requirements
- Monitor log file sizes and implement log rotation
- Configure separate log appenders for performance metrics

### 📈 Analysis
- Review performance reports regularly
- Monitor health scores for trend analysis
- Set up automated alerts for critical performance degradation

### 🧹 Maintenance
- Reset metrics periodically for long-running applications
- Clean up old alerts to prevent memory accumulation
- Monitor the metrics service itself for performance impact

## Troubleshooting

### Common Issues

#### High Memory Usage Alerts
```java
// Check for memory leaks
String report = metrics.getPerformanceReport();
// Look for operations with consistently high memory usage
```

#### Slow Operation Detection
```java
// Identify bottlenecks
// Review operations exceeding SLOW_OPERATION_THRESHOLD_MS
// Consider optimization or threshold adjustment
```

#### Thread Safety Concerns
```java
// Validate concurrent access patterns
// Ensure proper use of thread-safe collections
// Monitor for deadlocks or lock contention
```

### Performance Tuning

#### Threshold Adjustment
- Increase thresholds for systems with higher capacity
- Decrease thresholds for stricter performance requirements
- Consider operation-specific thresholds for complex systems

#### Memory Optimization
- Monitor heap utilization trends
- Implement operation-specific memory limits
- Consider garbage collection tuning for high-throughput systems

## Future Enhancements

### Planned Features
- Operation-specific threshold configuration
- Historical metrics persistence
- Performance trend analysis
- Integration with external monitoring systems
- Custom metric types and dimensions
- Performance regression detection

### Integration Opportunities
- REST API endpoints for metrics exposure
- Dashboard visualization components
- Alerting system integration
- Performance profiling tools integration

---

*This guide provides comprehensive information for implementing and using the Performance Metrics System. For specific implementation details, refer to the source code documentation and JavaDoc comments.*