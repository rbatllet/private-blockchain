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

### [Streaming Patterns Guide](STREAMING_PATTERNS_GUIDE.md)
**Streaming APIs and batch processing patterns**

Comprehensive guide to constant-memory streaming operations:
- Batch processing patterns (`processChainInBatches`)
- Streaming validation patterns (`validateChainStreaming`)
- Database-specific optimizations
- Phase B.2 specialized streaming methods
- Memory usage: constant ~50MB regardless of chain size

### [Concurrency Guide](CONCURRENCY_GUIDE.md)
**StampedLock patterns and thread-safety**

Essential guide for thread-safe blockchain operations:
- StampedLock usage patterns (optimistic/read/write locks)
- Dual-mode pattern for internal calls
- LockTracer debugging techniques
- UserFriendlyEncryptionAPI thread-safety
- Common deadlock scenarios and solutions

**⚠️ Critical**: StampedLock is NOT reentrant - nested locking causes deadlocks!

### [Large File Chunking Guide](LARGE_FILE_CHUNKING_GUIDE.md)
**Chunking strategies for files over 10MB**

Guide for handling large files that exceed the 10MB block size limit:
- Chunking strategies and algorithms
- Metadata management for multi-chunk files
- Reassembly patterns
- Best practices for large file storage

## Performance & Optimization

### [Java 21-25 Features Optimization Report](JAVA_21_25_FEATURES_OPTIMIZATION_REPORT.md)
**Java 21-25 features analysis and optimization opportunities**

Analysis of modern Java features for blockchain optimization:
- Virtual Threads evaluation
- Pattern matching improvements
- Record patterns
- Performance benchmarks and recommendations

### [Virtual Threads Benchmark Guide](VIRTUAL_THREADS_BENCHMARK_GUIDE.md)
**Benchmarking virtual threads vs platform threads**

Comprehensive benchmarking guide:
- Benchmark setup and execution
- Performance metrics collection
- Analysis of virtual threads benefits
- Migration recommendations

### [Virtual Threads Investigation Report](VIRTUAL_THREADS_INVESTIGATION_REPORT.md)
**Investigation results of virtual threads implementation**

Detailed investigation findings:
- Implementation analysis
- Performance characteristics
- Known issues and workarounds
- Best practices for virtual threads

### [Semaphore Indexing Implementation](SEMAPHORE_INDEXING_IMPLEMENTATION.md)
**Per-block semaphore coordination for concurrent indexing**

Technical implementation details:
- Fair semaphores for FIFO scheduling
- Per-block coordination patterns
- Race condition prevention
- Thread safety guarantees

## Quick Reference

| Guide | Use Case | Critical For |
|-------|----------|--------------|
| Memory Safety | Large blockchain access | Production deployments |
| Streaming Patterns | Constant-memory operations | Scalability |
| Concurrency | Multi-threaded operations | Thread safety |
| Large File Chunking | Files over 10MB | Large data handling |
| Java 21-25 Features | Modern Java optimization | Performance |
| Virtual Threads Benchmark | Thread performance testing | Scalability analysis |
| Virtual Threads Investigation | Virtual threads deep-dive | Implementation decisions |
| Semaphore Indexing | Concurrent indexing coordination | Thread safety |

## Related Documentation

- [Database Guides](../database/README.md) - JPA transactions and database operations
- [Testing Guide](../testing/TESTING.md) - Thread-safety testing standards
- [API Guide](../reference/API_GUIDE.md) - API usage examples
