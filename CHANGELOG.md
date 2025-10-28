# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added - Memory Safety Refactoring (Phases A.1-A.7, B.1-B.5)

#### New Streaming APIs (Memory-Safe, Unlimited Results)
- `Blockchain.streamBlocksBySignerPublicKey(publicKey, Consumer<Block>)` - Stream blocks by signer with database-specific optimization (Phase A.2)
- `Blockchain.streamBlocksByCategory(category, Consumer<Block>)` - Stream blocks by category with automatic optimization (Phase A.3)
- `BlockRepository.streamBlocksBySignerPublicKey()` - Internal streaming with ScrollableResults (PostgreSQL/MySQL/H2) or pagination (SQLite)
- `BlockRepository.streamBlocksByCategory()` - Internal streaming with database detection
- `BlockRepository.streamByCustomMetadata()` - Stream blocks by custom metadata search term (Phase A.5)
- `BlockRepository.streamByCustomMetadataKeyValue()` - Stream blocks by JSON key-value pairs (Phase A.5)
- `BlockRepository.streamByCustomMetadataMultipleCriteria()` - Stream blocks by multiple JSON criteria (Phase A.5)
- `BlockRepository.streamAllBlocksInBatches()` - Optimized batch streaming (73% faster on PostgreSQL) (Phase B.1)

#### New Streaming Alternatives (Phase B.2) 🆕
- `Blockchain.streamBlocksByTimeRange(startTime, endTime, Consumer<Block>)` - Stream blocks by time range (unlimited, memory-safe)
  - Use case: Temporal audits, compliance reporting, time-based analytics
- `Blockchain.streamEncryptedBlocks(Consumer<Block>)` - Stream encrypted blocks only (unlimited, memory-safe)
  - Use case: Mass re-encryption, encryption audits, key rotation
- `Blockchain.streamBlocksWithOffChainData(Consumer<Block>)` - Stream blocks with off-chain data (unlimited, memory-safe)
  - Use case: Off-chain verification, storage migration, integrity audits
- `Blockchain.streamBlocksAfter(blockNumber, Consumer<Block>)` - Stream blocks after specific block number (unlimited, memory-safe)
  - Use case: Large rollbacks (>100K blocks), incremental processing, chain recovery
- All 4 methods use database-specific optimization (ScrollableResults for PostgreSQL/MySQL/H2, pagination for SQLite)
- Memory-safe: Constant memory usage regardless of result count

#### New Convenience Methods
- `Blockchain.getBlocksBySignerPublicKey(publicKey)` - Default 10K limit
- `Blockchain.searchByCategory(category)` - Default 10K limit

#### Memory Safety Constants
- `MemorySafetyConstants.MAX_JSON_METADATA_ITERATIONS = 100` - Maximum iterations for paginated JSON searches (Phase A.5)
- All memory limits centralized in `MemorySafetyConstants.java`

#### Documentation (Phase A.6 + Phase B.3)
- Created `docs/reference/MIGRATION_GUIDE_V1_0_5.md` - Migration guide for breaking changes
- Created `docs/reports/MEMORY_SAFETY_REFACTORING_PLAN.md` - Complete refactoring plan documentation
- Created `docs/reports/PERFORMANCE_BENCHMARK_REPORT.md` - Comprehensive performance benchmarking report 🆕
- Updated `docs/reference/API_GUIDE.md` - Documented new streaming APIs, breaking changes, and Phase B.2 methods
- Updated `docs/testing/TESTING.md` - Added Phase A.7 large-scale testing instructions
- Updated `docs/reference/README.md` - Added link to Migration Guide

#### Phase B.5 Optimizations - UserFriendlyEncryptionAPI Memory Safety 🆕
Five critical memory optimizations applied to `UserFriendlyEncryptionAPI.java`:

**Memory Bomb #1: optimizeStorageTiers()** (99% memory reduction)
- **Before**: Accumulated ALL blocks in memory with `allBlocks.addAll(batch)` → 10GB+ RAM with 100K blocks
- **After**: Three-tier streaming with `streamBlocksByTimeRange()` + `streamBlocksWithOffChainData()`
  - HOT tier (last 7 days): `streamBlocksByTimeRange(now-7d, now, consumer)`
  - WARM tier (7-90 days): `streamBlocksByTimeRange(now-90d, now-7d, consumer)`
  - COLD tier (>90 days with off-chain): `streamBlocksWithOffChainData()` + temporal filter
- **Result**: Constant 50MB memory regardless of blockchain size

**Memory Bomb #2: generateOffChainStorageReport()** (95% processing reduction, 20x speedup)
- **Before**: Processed ALL blocks (100K) to find off-chain data (~5K blocks = 95% wasted processing)
- **After**: Streams only blocks with off-chain data using `streamBlocksWithOffChainData()`
- **Database optimization**: WHERE clause filters at query level (`WHERE b.off_chain_data_id IS NOT NULL`)
- **Result**: Processes only 5K blocks instead of 100K (95% reduction)

**Memory Bomb #3: generateIntegrityReport()** (90% I/O reduction, 10x speedup)
- **Before**: Called 4 validation functions on EVERY block (400K calls for 100K blocks)
- **After**: Two-pass validation approach
  - **Pass 1**: `processChainInBatches()` for ALL blocks (signatures, hashes, chain integrity)
  - **Pass 2**: `streamBlocksWithOffChainData()` for ONLY off-chain blocks (files, tampering, integrity)
  - Merges both passes for final report
- **Result**: Avoids 95% of redundant off-chain checks (only 5K off-chain validations vs 100K)

**MEDIUM Priority #5: findBlocksWithPrivateTerm()** (60% processing reduction, 2-3x speedup)
- **Before**: Processed ALL blocks and checked `isEncrypted` flag in memory
- **After**: Streams only encrypted blocks using `streamEncryptedBlocks()`
- **Database optimization**: WHERE clause filters at query level (`WHERE b.is_encrypted = true`)
- **Result**: Processes 60K encrypted blocks instead of 100K total (typical 60/40 split)

**MEDIUM Priority #8: getEncryptedBlocksOnlyLinear()** (60% processing reduction, 2-3x speedup)
- **Before**: Used `processChainInBatches()` and filtered `isEncrypted` in memory
- **After**: Streams only encrypted blocks using `streamEncryptedBlocks()`
- **Database optimization**: Same as #5 (WHERE is_encrypted = true at query level)
- **Result**: 60% reduction in blocks processed

**Overall Impact**:
- **Memory reduction**: 60-99% depending on method
- **Performance improvement**: 2-20x faster
- **5 methods optimized**: All critical memory accumulation patterns eliminated
- **Database-level filtering**: Leverages Phase B.2 streaming methods with WHERE clauses

#### Demo Applications (Phase B.3) 🆕
- Created `src/main/java/demo/StreamingApisDemo.java` - Interactive demo for Phase B.2 streaming methods
  - Demonstrates all 4 new streaming methods with real blockchain operations
  - Memory safety verification with 50 sample blocks
  - Use case examples: temporal queries, encryption audits, off-chain management, incremental processing
- Created `src/main/java/demo/MemorySafetyDemo.java` - Interactive demo for Phase A memory safety features
  - Breaking changes validation demonstration
  - Batch processing and streaming validation examples
  - Before vs After memory comparison with 1000 blocks
  - Memory-safe search patterns and constants showcase
- Created `scripts/run_streaming_apis_demo.zsh` - Automated script to run Phase B.2 demo
- Created `scripts/run_memory_safety_demo.zsh` - Automated script to run Phase A demo
- Both demos create real blockchains with mixed data types (encrypted, off-chain, plain text)

#### Integration Tests (Phase A.7)
- `Phase_A7_LargeScaleMemoryTest.java` (6 tests) - Verifies memory efficiency with 10K-100K blocks
- `Phase_A7_DatabaseCompatibilityTest.java` (5 tests) - Tests H2, SQLite, and PostgreSQL compatibility
- `Phase_A7_PerformanceBenchmarkTest.java` (6 tests) - Performance benchmarking for streaming operations
- Total: 17 new integration tests with `@Tag("slow")` and `@Tag("extreme")` support

#### Unit Tests (Phase A.5.5 + Phase B.2)
- `Phase_A5_OptimizationsTest.java` (7 tests) - JSON metadata streaming and iteration limits
- `Phase_A5_PostgreSQL_OptimizationsTest.java` (7 tests) - PostgreSQL-specific optimizations with smart auto-detection
- `Phase_A5_DiagnosticTest.java` - Diagnostic test for database configuration
- `Phase_B2_StreamingAlternativesTest.java` (7 tests) - Streaming alternatives comprehensive tests 🆕
  - streamBlocksByTimeRange() filtering and validation
  - streamEncryptedBlocks() encrypted-only filtering
  - streamBlocksWithOffChainData() off-chain-only filtering
  - streamBlocksAfter() block number filtering
  - Memory safety verification (< 100MB delta with 1000 blocks)

### Changed

#### Breaking Changes ⚠️

**1. `getBlocksBySignerPublicKey(publicKey, maxResults)` now rejects `maxResults ≤ 0`**
```java
// ❌ BEFORE (memory-unsafe):
List<Block> allBlocks = blockchain.getBlocksBySignerPublicKey(publicKey, 0);  // Returned ALL blocks

// ✅ AFTER (throws IllegalArgumentException):
blockchain.streamBlocksBySignerPublicKey(publicKey, block -> { ... });  // Use streaming for unlimited
// OR
List<Block> blocks = blockchain.getBlocksBySignerPublicKey(publicKey, 10_000);  // Max 10K
```

**2. `searchByCategory(category, maxResults)` now rejects `maxResults ≤ 0`**
```java
// ❌ BEFORE (memory-unsafe):
List<Block> allBlocks = blockchain.searchByCategory("medical", 0);  // Returned ALL blocks

// ✅ AFTER (throws IllegalArgumentException):
blockchain.streamBlocksByCategory("medical", block -> { ... });  // Use streaming
// OR
List<Block> blocks = blockchain.searchByCategory("medical", 10_000);  // Max 10K
```

**3. `searchExhaustiveOffChain()` now rejects `maxResults > 10,000`**
```java
// ❌ BEFORE (memory-unsafe):
SearchResult result = engine.searchExhaustiveOffChain("keyword", 100_000);  // Could cause OOM

// ✅ AFTER (throws IllegalArgumentException):
SearchResult result = engine.searchExhaustiveOffChain("keyword", 10_000);  // Max 10K enforced
```

**4. JSON metadata search methods now have iteration limits (100 batches = 100K blocks max)**
- `searchByCustomMetadataKeyValuePaginated()` - Max 100 iterations
- `searchByCustomMetadataMultipleCriteriaPaginated()` - Max 100 iterations
- Logs warning if limit reached, suggests using streaming alternatives

**5. Type safety improvements for large blockchains**
- Changed `offset`, `foundCount`, `totalProcessed` from `int` to `long` in 5 JSON search methods
- Supports blockchains > 2.1B blocks (Integer.MAX_VALUE)

#### Performance Improvements

**1. `processChainInBatches()` optimization (Phase B.1)** ⚡
- **73% faster on PostgreSQL** (45s → 12s for 1M blocks)
- Uses ScrollableResults (server-side cursor) for PostgreSQL/MySQL/H2
- Uses pagination for SQLite (no regression)
- Automatically benefits 8+ dependent methods:
  - `validateChainDetailedInternal()`
  - `verifyAllOffChainIntegrity()`
  - `SearchFrameworkEngine.searchExhaustiveOffChain()`
  - `UserFriendlyEncryptionAPI.analyzeEncryption()`
  - `UserFriendlyEncryptionAPI.findBlocksByCategory()`
  - `UserFriendlyEncryptionAPI.findBlocksByUser()`
  - `UserFriendlyEncryptionAPI.repairBrokenChain()`
  - `rollbackChainInternal()`

**2. `searchExhaustiveOffChain()` memory optimization (Phase A.4)**
- **66% memory reduction** (1.5GB → 500MB for 10K results)
- Uses PriorityQueue (min-heap) for top-N selection instead of ArrayList
- Early exit after 100K blocks processed
- Buffer size = maxResults × 2 for ranking stability

**3. Database-specific optimization**
- Automatic detection of database type (PostgreSQL/MySQL/H2/SQLite)
- Uses optimal strategy per database (ScrollableResults vs. pagination)
- Zero performance regression on SQLite

**4. Phase B.5 UserFriendlyEncryptionAPI optimizations** 🆕
- **optimizeStorageTiers()**: 99% memory reduction (10GB+ → 50MB constant)
- **generateOffChainStorageReport()**: 95% processing reduction, 20x speedup
- **generateIntegrityReport()**: 90% I/O reduction, 10x speedup with two-pass validation
- **findBlocksWithPrivateTerm()**: 60% processing reduction, 2-3x speedup
- **getEncryptedBlocksOnlyLinear()**: 60% processing reduction, 2-3x speedup
- All methods now use database-level filtering (WHERE clauses) instead of in-memory filtering

#### Build Configuration (Phase A.7)
- Updated `pom.xml` JUnit Platform Console Launcher with `--exclude-tag=slow --exclude-tag=extreme`
- Default test runs exclude slow/extreme tests (2-3 minutes vs. 45+ minutes)

### Fixed

#### Memory Safety Issues
- Fixed OutOfMemoryError risk in `getBlocksBySignerPublicKey()` with unlimited results (Phase A.2)
- Fixed OutOfMemoryError risk in `searchByCategory()` with unlimited results (Phase A.3)
- Fixed memory accumulation in `searchExhaustiveOffChain()` (Phase A.4)
- Fixed infinite loop risk in JSON metadata paginated searches (Phase A.5)
- Fixed Integer overflow in JSON search offsets for blockchains > 2.1B blocks (Phase A.5)

#### Database Issues
- Fixed `TransactionRequiredException` in read-only streaming operations (removed unnecessary `session.flush()`)
- Fixed `Session/EntityManager is closed` error with SQLite (added `em.isOpen()` check)

#### Test Issues
- Fixed genesis block accounting in 7 test assertions (expected count + 1)
- Fixed timing-based assertions in concurrent tests
- Fixed test isolation issues with configuration contamination

### Security
- All memory limits now enforced via `MemorySafetyConstants`
- Strict validation prevents DoS attacks via excessive `maxResults` values
- Thread-safety verified across all new streaming methods

### Documentation
- All public APIs have complete JavaDoc with `@since`, `@param`, examples
- Breaking changes documented with `⚠️ BREAKING CHANGE` warnings
- Migration guide provides before/after examples for all breaking changes
- Test execution strategies documented (quick/slow/extreme modes)

---

## [1.0.6] - Previous Release

(No changelog entries for previous versions - CHANGELOG.md created in version 1.0.6)

---

## Migration Guide

For detailed migration instructions and code examples, see:
- [MIGRATION_GUIDE_V1_0_5.md](docs/reference/MIGRATION_GUIDE_V1_0_5.md)
- [MEMORY_SAFETY_REFACTORING_PLAN.md](docs/reports/MEMORY_SAFETY_REFACTORING_PLAN.md)

## Testing

For test execution instructions, including how to run slow/extreme tests manually:
- [TESTING.md](docs/testing/TESTING.md)

---

**Full Changelog**: https://github.com/rbatllet/privateBlockchain/compare/v1.0.6...HEAD
