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

## Quick Reference

| Guide | Use Case | Critical For |
|-------|----------|--------------|
| Memory Safety | Large blockchain access | Production deployments |
| Streaming Patterns | Constant-memory operations | Scalability |
| Concurrency | Multi-threaded operations | Thread safety |

## Related Documentation

- [Database Guides](../database/README.md) - JPA transactions and database operations
- [Testing Guide](../testing/TESTING.md) - Thread-safety testing standards
- [API Guide](../reference/API_GUIDE.md) - API usage examples
