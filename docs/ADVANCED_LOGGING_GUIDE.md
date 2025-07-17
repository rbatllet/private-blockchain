# Advanced Logging System Guide

## Overview

The Advanced Logging System provides comprehensive operation tracking, performance monitoring, and contextual logging for blockchain operations with enterprise-grade features.

## Architecture

### Core Components

#### AdvancedLoggingService
- **Operation context tracking** with unique operation IDs
- **MDC (Mapped Diagnostic Context)** for request correlation
- **Performance monitoring** with detailed operation metrics
- **Hierarchical operation support** for complex workflows

#### LoggingManager
- **Central coordinator** for all logging services
- **Automatic integration** with blockchain operations
- **Lifecycle management** for logging components
- **Unified reporting** across all subsystems

#### OperationLoggingInterceptor
- **Annotation-driven logging** with `@OperationLogger`
- **Automatic method instrumentation** for seamless integration
- **Context propagation** across method calls
- **Exception handling** with detailed error logging

## Features

### 🔍 Operation Tracking
- Unique operation IDs for request correlation
- Hierarchical operation context (parent-child relationships)
- Start/end time tracking with precise duration calculation
- Result counting and success/failure status

### 📊 Performance Monitoring
- Response time measurement and analysis
- Operation success/failure rate tracking
- Performance report generation with detailed metrics
- Integration with PerformanceMetricsService

### 🏷️ Contextual Logging
- MDC-based context propagation
- Custom context attributes per operation
- Thread-safe context management
- Automatic context cleanup

### 📋 Comprehensive Reporting
- Operation performance summaries
- Success/failure statistics
- Context information inclusion
- Formatted reports with operation details

## Usage

### Automatic Integration via LoggingManager

```java
// Blockchain operations with automatic logging
String result = LoggingManager.logBlockchainOperation(
    "VALIDATION", 
    "Block validation operation",
    blockSize,
    () -> validateBlock(block)
);

// Search operations with automatic logging
List<Block> results = LoggingManager.logSearchOperation(
    "ENCRYPTED_SEARCH",
    searchParams,
    () -> performEncryptedSearch(query)
);
```

### Manual Operation Tracking

```java
// Start operation with context
Map<String, String> context = new HashMap<>();
context.put("userId", "user123");
context.put("operation", "blockValidation");

String operationId = AdvancedLoggingService.startOperation(
    "VALIDATION", 
    "Manual block validation", 
    context
);

try {
    // Perform operation
    boolean result = validateBlock(block);
    
    // End operation successfully
    AdvancedLoggingService.endOperation(operationId, true, 1, null);
    
} catch (Exception e) {
    // End operation with failure
    AdvancedLoggingService.endOperation(operationId, false, 0, e.getMessage());
    throw e;
}
```

### Annotation-Based Logging

```java
@Component
public class BlockchainService {
    
    @OperationLogger(
        operationType = "BLOCKCHAIN",
        operationName = "addBlock",
        includeArgs = true,
        includeResult = true
    )
    public Block addBlock(String data) {
        // Method implementation
        return new Block(data);
    }
    
    @OperationLogger(
        operationType = "SEARCH",
        operationName = "findBlocks"
    )
    public List<Block> findBlocks(String criteria) {
        // Method implementation
        return searchBlocks(criteria);
    }
}
```

## Configuration

### Logback Configuration

The system uses separate appenders for different log types:

```xml
<!-- Advanced Logging Appender -->
<appender name="ADVANCED_LOGGING" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/advanced-logging-${ENV:-development}.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/archived/advanced-logging-${ENV:-development}-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{operationId}] %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<!-- Performance Metrics Appender -->
<appender name="PERFORMANCE_METRICS" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/performance-metrics-${ENV:-development}.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/archived/performance-metrics-${ENV:-development}-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>50MB</maxFileSize>
        <maxHistory>15</maxHistory>
        <totalSizeCap>5GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<!-- Security Events Appender -->
<appender name="SECURITY_EVENTS" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/security-events-${ENV:-development}.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/archived/security-events-${ENV:-development}-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>200MB</maxFileSize>
        <maxHistory>90</maxHistory>
        <totalSizeCap>20GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{operationId}] %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

### Logger Configuration

```xml
<!-- Advanced Logging Service -->
<logger name="com.rbatllet.blockchain.logging.AdvancedLoggingService" level="INFO" additivity="false">
    <appender-ref ref="ADVANCED_LOGGING"/>
    <appender-ref ref="CONSOLE"/>
</logger>

<!-- Performance Metrics -->
<logger name="performance.metrics" level="INFO" additivity="false">
    <appender-ref ref="PERFORMANCE_METRICS"/>
</logger>

<!-- Security Events -->
<logger name="security.events" level="INFO" additivity="false">
    <appender-ref ref="SECURITY_EVENTS"/>
</logger>
```

## Operation Context

### Context Attributes

The system automatically includes standard context attributes:

```java
// Standard context attributes
Map<String, String> context = new HashMap<>();
context.put("operationType", operationType);
context.put("operationName", operationName);
context.put("timestamp", LocalDateTime.now().toString());
context.put("threadId", String.valueOf(Thread.currentThread().getId()));
context.put("threadName", Thread.currentThread().getName());

// Custom attributes can be added
context.put("userId", getCurrentUserId());
context.put("requestId", getRequestId());
context.put("dataSize", String.valueOf(dataSize));
```

### MDC Integration

```java
// MDC is automatically managed
MDC.put("operationId", operationId);
MDC.put("operationType", operationType);
MDC.put("threadName", Thread.currentThread().getName());

// Context is automatically cleared after operation
try {
    // Operation execution
} finally {
    MDC.clear(); // Automatic cleanup
}
```

## Reporting

### Performance Report Generation

```java
// Get comprehensive performance report
String report = AdvancedLoggingService.getPerformanceReport();

// Example output:
/*
🚀 ADVANCED LOGGING PERFORMANCE REPORT
=====================================
Generated: 2024-01-15 14:30:45
Report Period: Last 24 hours

📊 OPERATION SUMMARY
────────────────────
Total Operations: 1,247
Successful Operations: 1,198 (96.1%)
Failed Operations: 49 (3.9%)
Average Duration: 156ms

🔍 OPERATIONS BY TYPE
────────────────────
BLOCKCHAIN: 423 operations (avg: 234ms)
SEARCH: 567 operations (avg: 89ms)
VALIDATION: 257 operations (avg: 312ms)

⚡ PERFORMANCE METRICS
────────────────────
Fastest Operation: 12ms (SEARCH)
Slowest Operation: 5,234ms (VALIDATION)
95th Percentile: 445ms
99th Percentile: 1,203ms

🚨 ERROR ANALYSIS
────────────────
Most Common Errors:
- ValidationException: 23 occurrences
- TimeoutException: 15 occurrences
- CryptoException: 11 occurrences
*/
```

### System Status Report

```java
// Get integrated system report via LoggingManager
String systemReport = LoggingManager.getSystemReport();

// Includes:
// - Advanced logging metrics
// - Performance metrics
// - Search metrics
// - System health summary
```

## Operation Lifecycle

### Operation States

1. **STARTED**: Operation initiated with context setup
2. **IN_PROGRESS**: Operation executing (automatic MDC management)
3. **COMPLETED**: Operation finished successfully
4. **FAILED**: Operation ended with error
5. **TIMEOUT**: Operation exceeded time limits (if configured)

### Lifecycle Events

```java
// Operation start
String operationId = AdvancedLoggingService.startOperation(type, name, context);
// 📋 OPERATION_START [VALIDATION] - Block validation operation (ID: op_20240115_143045_001)

// Operation progress (optional)
AdvancedLoggingService.updateOperationContext(operationId, "progress", "50%");
// 📋 OPERATION_UPDATE [VALIDATION] - Progress: 50% (ID: op_20240115_143045_001)

// Operation end
AdvancedLoggingService.endOperation(operationId, true, 1, null);
// ✅ OPERATION_END [VALIDATION] - Completed successfully in 234ms (ID: op_20240115_143045_001)
```

## Thread Safety

### Concurrent Operations
- **Thread-safe operation tracking** with `ConcurrentHashMap`
- **MDC management** per thread context
- **Atomic operation counters** for performance metrics
- **Lock-free context updates** where possible

### Context Isolation
- **Thread-local MDC** ensures context isolation
- **Operation-specific contexts** prevent cross-contamination
- **Automatic cleanup** prevents memory leaks

## Integration Examples

### Blockchain Operations

```java
// Comprehensive blockchain operation logging
public Block addBlockWithLogging(String data) {
    return LoggingManager.logBlockchainOperation(
        "ADD_BLOCK",
        "Adding new block to blockchain",
        data.length(),
        () -> {
            // Validate data
            validateBlockData(data);
            
            // Create block
            Block newBlock = new Block(data);
            
            // Add to chain
            blockchain.addBlock(newBlock);
            
            return newBlock;
        }
    );
}
```

### Search Operations

```java
// Search operation with detailed logging
public List<Block> searchWithLogging(String criteria) {
    Map<String, String> searchContext = new HashMap<>();
    searchContext.put("searchCriteria", criteria);
    searchContext.put("searchType", "encrypted");
    
    return LoggingManager.logSearchOperation(
        "ENCRYPTED_SEARCH",
        searchContext,
        () -> performEncryptedSearch(criteria)
    );
}
```

### Custom Operations

```java
// Manual operation logging for complex workflows
public void complexWorkflowWithLogging() {
    Map<String, String> context = new HashMap<>();
    context.put("workflowType", "batch_processing");
    context.put("batchSize", "100");
    
    String operationId = AdvancedLoggingService.startOperation(
        "WORKFLOW", 
        "Batch processing workflow", 
        context
    );
    
    try {
        // Step 1: Data preparation
        AdvancedLoggingService.updateOperationContext(operationId, "step", "data_preparation");
        prepareData();
        
        // Step 2: Processing
        AdvancedLoggingService.updateOperationContext(operationId, "step", "processing");
        processData();
        
        // Step 3: Validation
        AdvancedLoggingService.updateOperationContext(operationId, "step", "validation");
        validateResults();
        
        AdvancedLoggingService.endOperation(operationId, true, 100, null);
        
    } catch (Exception e) {
        AdvancedLoggingService.endOperation(operationId, false, 0, e.getMessage());
        throw e;
    }
}
```

## Performance Impact

### Minimal Overhead
- **Efficient context management** with minimal memory footprint
- **Asynchronous logging** to prevent blocking operations
- **Lazy evaluation** for expensive report generation
- **Optimized data structures** for high-throughput scenarios

### Scalability Features
- **Lock-free operation tracking** for high concurrency
- **Bounded collections** to prevent memory exhaustion
- **Configurable retention policies** for log management
- **Efficient cleanup** of completed operations

## Best Practices

### 🎯 Effective Usage
- Use `LoggingManager` automatic integration when possible
- Add meaningful context attributes for better traceability
- Include operation-specific metadata for debugging
- Use hierarchical operations for complex workflows

### 🔧 Configuration
- Configure appropriate log levels for different environments
- Set up log rotation to manage disk space
- Use separate appenders for different log types
- Configure MDC cleanup to prevent memory leaks

### 📈 Monitoring
- Review performance reports regularly
- Monitor log file sizes and rotation
- Set up alerts for error rate thresholds
- Analyze operation patterns for optimization opportunities

### 🧹 Maintenance
- Clean up old log files regularly
- Monitor the logging system's own performance
- Review and update context attributes periodically
- Validate log format compatibility with analysis tools

## Troubleshooting

### Common Issues

#### Missing Operation Context
```java
// Ensure proper operation lifecycle
String operationId = AdvancedLoggingService.startOperation(...);
try {
    // Operation code
} finally {
    // Always end operation to prevent context leaks
    AdvancedLoggingService.endOperation(operationId, success, count, error);
}
```

#### Memory Leaks
```java
// Check for proper MDC cleanup
// Review operation completion rates
// Monitor context map sizes
String report = AdvancedLoggingService.getPerformanceReport();
// Look for operations without corresponding end events
```

#### Performance Impact
```java
// Monitor logging overhead
// Consider asynchronous appenders for high-throughput scenarios
// Review log levels in production environments
```

## Testing

### Unit Tests
- Comprehensive test suite with operation lifecycle validation
- Context management testing with concurrent operations
- Performance impact measurement
- Error handling and cleanup verification

### Integration Tests
- End-to-end operation logging validation
- LoggingManager integration testing
- Multi-threaded context isolation testing
- Log output format validation

## Future Enhancements

### Planned Features
- Distributed tracing integration (Zipkin, Jaeger)
- Custom operation metrics and dimensions
- Real-time operation monitoring dashboard
- Advanced filtering and search capabilities
- Integration with external monitoring systems

### Performance Optimizations
- Asynchronous context propagation
- Batch operation reporting
- Compressed log storage
- Historical data archiving

---

*This guide provides comprehensive information for implementing and using the Advanced Logging System. For specific implementation details, refer to the source code documentation and JavaDoc comments.*