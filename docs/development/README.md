# Development Guides

This directory contains essential development guides for working with the blockchain codebase.

## Core Development Guides

### [Memory Safety Guide](MEMORY_SAFETY_GUIDE.md)
**Memory-efficient blockchain access patterns**

Critical guide for avoiding memory exhaustion when working with large blockchains. Covers:
- Memory-safe access patterns (batch processing, streaming)
- Anti-patterns to avoid (loading all blocks into memory)
- Memory safety constants and limits
- Best practices for production deployments

**⚠️ MUST READ** before working with blockchain data access.

### [Concurrency Guide](CONCURRENCY_GUIDE.md)
**StampedLock patterns and thread-safety**

Essential guide for thread-safe blockchain operations:
- StampedLock usage patterns (optimistic/read/write locks)
- Dual-mode pattern for internal calls
- LockTracer debugging techniques
- UserFriendlyEncryptionAPI thread-safety
- Common deadlock scenarios and solutions

**⚠️ Critical**: StampedLock is NOT reentrant - nested locking causes deadlocks!

## Performance & Optimization

### [Virtual Threads Benchmark Guide](VIRTUAL_THREADS_BENCHMARK_GUIDE.md)
**Benchmarking virtual threads vs platform threads**

Comprehensive benchmarking guide:
- Benchmark setup and execution
- Performance metrics collection
- Analysis of virtual threads benefits
- Migration recommendations

## Quick Reference

| Guide | Use Case | Critical For |
|-------|----------|--------------|
| Memory Safety | Large blockchain access | Production deployments |
| Concurrency | Multi-threaded operations | Thread safety |
| Virtual Threads Benchmark | Thread performance testing | Scalability analysis |

## Related Documentation

### Documents Moved to More Specific Categories

The following documents have been reorganized for better discoverability:

- **[Streaming Patterns Guide](../data-management/STREAMING_PATTERNS_GUIDE.md)** → Moved to `docs/data-management/`
- **[Large File Chunking Guide](../data-management/LARGE_FILE_CHUNKING_GUIDE.md)** → Moved to `docs/data-management/`
- **[Semaphore Indexing Implementation](../search/SEMAPHORE_INDEXING_IMPLEMENTATION.md)** → Moved to `docs/search/`
- **[Java 21-25 Features Report](../reports/JAVA_21_25_FEATURES_OPTIMIZATION_REPORT.md)** → Moved to `docs/reports/`
- **[Virtual Threads Investigation Report](../reports/VIRTUAL_THREADS_INVESTIGATION_REPORT.md)** → Moved to `docs/reports/`

### Other Resources

- [Database Guides](../database/README.md) - JPA transactions and database operations
- [Data Management](../data-management/README.md) - Batch processing, streaming, and large files
- [Search Guides](../search/README.md) - Search framework and indexing
- [Reports](../reports/README.md) - Technical investigation reports
- [Testing Guide](../testing/TESTING.md) - Thread-safety testing standards
- [API Guide](../reference/API_GUIDE.md) - API usage examples

---

**Directory**: `docs/development/`
**Files**: 4 (3 guides + README)
**Last Updated**: 2026-01-24
