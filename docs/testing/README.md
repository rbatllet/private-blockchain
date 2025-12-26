# Testing Documentation

This directory contains comprehensive testing guides, thread-safety standards, and concurrent programming patterns for the Private Blockchain.

## 📚 Documents in This Directory (5 files)

### 🎯 Essential Guides
| Document | Description | Recommended For |
|----------|-------------|-----------------|
| **[TESTING.md](TESTING.md)** | Complete testing guide with 828+ tests | **START HERE** - All developers |
| **[THREAD_SAFETY_STANDARDS.md](THREAD_SAFETY_STANDARDS.md)** | Concurrency best practices and patterns | Thread-safe code |

### 🔒 Thread-Safety Patterns
| Document | Description | Recommended For |
|----------|-------------|-----------------|
| **[THREAD_SAFETY_TESTS.md](THREAD_SAFETY_TESTS.md)** | Thread-safety testing guide | Concurrent testing |
| **[SHARED_STATE_TESTING_PATTERNS.md](SHARED_STATE_TESTING_PATTERNS.md)** | Testing static/singleton shared state | Complex test scenarios |
| **[Thread Safety & Semaphores](../development/SEMAPHORE_INDEXING_IMPLEMENTATION.md)** | Per-block semaphore coordination | Concurrent indexing |

## 🚀 Recommended Reading Order

### For New Developers
1. **[TESTING.md](TESTING.md)** - Learn the test suite structure
2. **[THREAD_SAFETY_STANDARDS.md](THREAD_SAFETY_STANDARDS.md)** - Understand concurrency patterns
3. **[THREAD_SAFETY_TESTS.md](THREAD_SAFETY_TESTS.md)** - Write thread-safe tests

### For Code Reviewers
1. **[THREAD_SAFETY_STANDARDS.md](THREAD_SAFETY_STANDARDS.md)** - Review concurrency patterns
2. **[SHARED_STATE_TESTING_PATTERNS.md](SHARED_STATE_TESTING_PATTERNS.md)** - Validate test isolation
3. **[Thread Safety & Concurrent Indexing](../monitoring/INDEXING_COORDINATOR_EXAMPLES.md#thread-safety--concurrent-indexing)** - Check concurrent indexing safety

## 📊 Test Suite Overview

### Statistics
- **Total Tests**: 828+ tests
- **Code Coverage**: 72%+
- **Execution Time**: ~34 seconds (all tests)
- **Thread-Safety Tests**: 18+ test files
- **Success Rate**: 100% (all tests passing)

### Test Categories
1. **Core Tests** - Basic blockchain operations
2. **Advanced Tests** - Complex scenarios
3. **Thread-Safety Tests** - Concurrent operations
4. **Security Tests** - Encryption and access control
5. **Integration Tests** - End-to-end workflows
6. **Performance Tests** - Optimization validation

**See**: [TESTING.md](TESTING.md) for complete structure

## 🔒 Thread-Safety Architecture

### Lock Implementation
- **Primary Lock**: `StampedLock` (Java 8+)
- **Pattern**: Optimistic reads + conservative locks
- **Performance**: ~50% faster than ReentrantReadWriteLock
- **Critical**: NOT reentrant (requires careful design)

**See**: [THREAD_SAFETY_STANDARDS.md](THREAD_SAFETY_STANDARDS.md) for patterns

### Lock Patterns

#### Optimistic Read (Best Performance)
```java
// See: THREAD_SAFETY_STANDARDS.md
long stamp = GLOBAL_BLOCKCHAIN_LOCK.tryOptimisticRead();
Block block = blockchain.getBlockByNumber(blockNumber);

if (!GLOBAL_BLOCKCHAIN_LOCK.validate(stamp)) {
    // Retry with read lock
    stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
    try {
        block = blockchain.getBlockByNumber(blockNumber);
    } finally {
        GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
    }
}
```

#### Conservative Read Lock
```java
long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
try {
    return blockchain.someReadOperation();
} finally {
    GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
}
```

#### Write Lock
```java
long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
try {
    blockchain.someWriteOperation();
} finally {
    GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
}
```

## 🎯 Running Tests

### All Tests (Recommended)
```bash
./scripts/run_all_tests.zsh              # ZSH script (optimized)
mvn exec:exec@run-tests                   # Maven exec plugin
mvn test                                  # Standard (may timeout on large tests)
```

### Specific Test Categories
```bash
./scripts/run_basic_tests.zsh            # Core tests
./scripts/run_advanced_tests.zsh         # Advanced tests
./scripts/run_advanced_thread_safety_tests.zsh  # Thread-safety tests
```

### Single Test Class
```bash
mvn test -Dtest=UserFriendlyEncryptionAPIPhase1Test
```

**See**: [TESTING.md](TESTING.md) for complete commands

## 🛡️ Thread-Safety Best Practices

### ✅ DO
- Use `StampedLock` for all blockchain operations
- Use optimistic reads for hot-path operations
- Keep critical sections as short as possible
- Use atomic types for independent counters
- Synchronized blocks for multi-field atomicity
- Test with 100+ concurrent threads

### ❌ DON'T
- Nest lock acquisitions (StampedLock is NOT reentrant)
- Hold locks during I/O operations
- Use `AtomicReference` for multi-field state without synchronization
- Call public methods from within locked code
- Skip thread-safety tests

**See**: [THREAD_SAFETY_STANDARDS.md](THREAD_SAFETY_STANDARDS.md) for complete checklist

## 🔬 Advanced Testing Patterns

### Testing Shared State
```java
// See: SHARED_STATE_TESTING_PATTERNS.md
@Execution(ExecutionMode.SAME_THREAD)  // Force sequential execution
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SharedStateTest {
    @Test
    @Order(1)
    void testInitialization() { /* ... */ }
}
```

### Multi-Instance Coordination
```java
// See: Thread Safety & Concurrent Indexing in INDEXING_COORDINATOR_EXAMPLES.md
SearchFrameworkEngine instance1 = new SearchFrameworkEngine(blockchain);
SearchFrameworkEngine instance2 = new SearchFrameworkEngine(blockchain);

// Both instances coordinate via atomic operations
```

### Stress Testing
```java
// See: THREAD_SAFETY_TESTS.md (Java 25 Virtual Threads)
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
for (int i = 0; i < 100; i++) {
    executor.submit(() -> {
        // Concurrent operations
    });
}
```

## 📈 Test Coverage

| Component | Coverage | Tests |
|-----------|---------|-------|
| **Core Blockchain** | 85%+ | 200+ tests |
| **Encryption API** | 90%+ | 150+ tests |
| **Search Framework** | 75%+ | 100+ tests |
| **Security** | 80%+ | 80+ tests |
| **Recovery** | 70%+ | 50+ tests |
| **Overall** | **72%+** | **828+ tests** |

**See**: [TESTING.md](TESTING.md) for detailed coverage

## 🔗 Related Documentation

- **[../reference/API_GUIDE.md](../reference/API_GUIDE.md)** - Thread-safety guarantees
- **[../reports/STAMPEDLOCK_AUDIT_REPORT.md](../reports/STAMPEDLOCK_AUDIT_REPORT.md)** - Lock migration audit
- **[../reports/ATOMIC_REFERENCE_AUDIT_REPORT.md](../reports/ATOMIC_REFERENCE_AUDIT_REPORT.md)** - Atomicity audit
- **[../getting-started/TROUBLESHOOTING_GUIDE.md](../getting-started/TROUBLESHOOTING_GUIDE.md)** - Test troubleshooting

## ⚠️ Critical Test Issues (Fixed)

### StampedLock Migration (October 2025)
- **Issue**: 13 deadlocks due to non-reentrant locks
- **Solution**: Dual-mode pattern, lock elimination
- **Status**: ✅ Fixed - 100% tests passing

**See**: [../reports/STAMPEDLOCK_MIGRATION_DEADLOCKS.md](../reports/STAMPEDLOCK_MIGRATION_DEADLOCKS.md)

### AtomicReference Atomicity (October 2025)
- **Issue**: Username/keyPair mismatch in concurrent scenarios
- **Solution**: Synchronized blocks for multi-field atomicity
- **Status**: ✅ Fixed - 100% success rate

**See**: [../reports/ATOMIC_REFERENCE_AUDIT_REPORT.md](../reports/ATOMIC_REFERENCE_AUDIT_REPORT.md)

---

**Directory**: `docs/testing/`
**Files**: 5
**Last Updated**: 2025-10-04
