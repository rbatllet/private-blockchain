# Shared State Testing Patterns Guide

## Overview
This document describes patterns and solutions for testing components with shared static state, based on lessons learned from debugging `SearchStatisticsDiscrepancyTest` failures.

## Problem: Cross-Test State Contamination

### Root Cause
When components use **shared static state** between instances, test executions can contaminate each other, leading to:
- Incorrect statistics reporting
- "Already indexed/processing" skip behaviors
- Non-deterministic test failures
- Tests that pass individually but fail in suite execution

### Identified Patterns in PrivateBlockchain

#### 1. SearchFrameworkEngine Global Processing Map
```java
// Static shared state across all SearchFrameworkEngine instances
private static final ConcurrentHashMap<String, BlockIndexingMetadata> globalProcessingMap = new ConcurrentHashMap<>();
```

**Impact**: Blocks marked as "already processed" in one test remain marked for subsequent tests.

#### 2. IndexingCoordinator Singleton State
```java
// Singleton with execution timing state
private final ConcurrentHashMap<String, Long> indexingProgress = new ConcurrentHashMap<>();
```

**Impact**: Time-based duplicate prevention affects fresh indexing operations in subsequent tests.

## Solution Pattern: Dual-State Cleanup

### Method-Level State Reset
For tests that create new instances requiring fresh state:

```java
@Test
public void testDirectEngineStatistics() throws Exception {
    // CRITICAL: Clear both coordination systems at method start
    IndexingCoordinator.getInstance().reset();
    SearchFrameworkEngine.clearGlobalProcessingMapForTesting();
    
    // Test execution with fresh state guaranteed
    SearchFrameworkEngine directEngine = new SearchFrameworkEngine();
    // ... rest of test
}
```

### Comprehensive Lifecycle Management
```java
@BeforeEach
public void setUp() throws Exception {
    // Initialize blockchain first
    blockchain = new Blockchain(keyManager);
    
    // Reset coordination after blockchain setup
    IndexingCoordinator.getInstance().reset();
    IndexingCoordinator.getInstance().enableTestMode();
}

@AfterEach  
public void tearDown() throws Exception {
    // Shutdown instances gracefully
    if (directEngine != null) {
        directEngine.shutdown();
    }
    
    // Clear static shared state
    SearchFrameworkEngine.clearGlobalProcessingMapForTesting();
    
    // Reset singleton state
    IndexingCoordinator.getInstance().reset();
    IndexingCoordinator.getInstance().disableTestMode();
}
```

## Best Practices

### 1. Identify Shared State Dependencies

When writing tests for components with cross-instance coordination:
- Map all static variables and singleton dependencies
- Document state sharing mechanisms
- Identify potential contamination points

### 2. Implement Testing Support Methods
Add cleanup methods to components with shared state:
```java
// Example from SearchFrameworkEngine
@VisibleForTesting
public static void clearGlobalProcessingMapForTesting() {
    globalProcessingMap.clear();
    logger.info("🧪 TESTING: Global processing map cleared");
}
```

### 3. Test Mode Controls
Implement test-specific behavior in singletons:
```java
// Example from IndexingCoordinator  
public void enableTestMode() {
    this.testMode = true;
    logger.info("🧪 Test mode enabled");
}

public void reset() {
    if (testMode) {
        indexingProgress.clear();
        logger.info("🔄 IndexingCoordinator reset completed in test mode");
    }
}
```

### 4. State Cleanup Strategy Decision Tree
```
Is test creating new instances? 
├─ YES: Method-level cleanup required
│   ├─ Clear static state at test start  
│   └─ Comprehensive tearDown cleanup
└─ NO: Standard lifecycle cleanup sufficient
    └─ @BeforeEach/@AfterEach cleanup
```

## Warning Signs

Watch for these indicators of shared state contamination:

### Test Behavior Symptoms
- ✗ Tests pass individually but fail in suite
- ✗ Non-deterministic failures based on execution order
- ✗ Statistics showing unexpected values (0, partial counts)
- ✗ "Already processed" or "skip" log messages in fresh tests

### Log Pattern Analysis
Look for cross-contamination evidence:
```
// Bad: Previous test state affecting new test
[TEST-2] ⏭️ SKIPPED (already indexed/processing): abc123 | Existing: REAL_METADATA

// Good: Fresh state in each test  
[TEST-1] ✅ SUCCESSFULLY INDEXED: abc123
[TEST-2] ✅ SUCCESSFULLY INDEXED: abc123 (same block, fresh state)
```

## Implementation Checklist

- [ ] Identify all shared static state in component dependencies
- [ ] Add `@VisibleForTesting` cleanup methods for static state
- [ ] Implement test mode controls in singletons
- [ ] Add method-level state reset for instance-creating tests
- [ ] Verify cleanup in @AfterEach methods
- [ ] Test execution order independence
- [ ] Document shared state dependencies for future developers

## Related Components

### Current Implementation
- `SearchFrameworkEngine`: Static globalProcessingMap cleanup
- `IndexingCoordinator`: Singleton reset with test mode
- `SearchStatisticsDiscrepancyTest`: Reference implementation

### Future Considerations
Apply this pattern to any components with:
- Static caches or coordination maps
- Singleton state management  
- Cross-instance coordination mechanisms
- Time-based duplicate prevention
- Global configuration state

---
*Last updated: September 12, 2025*
*Based on SearchStatisticsDiscrepancyTest debugging session*