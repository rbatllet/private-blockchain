# 📊 Search Metrics Persistence Implementation Guide

## Table of Contents

- [Overview](#overview)
- [Problem Statement](#problem-statement)
- [Solution Architecture](#solution-architecture)
- [Entity Design](#entity-design)
- [Implementation Details](#implementation-details)
- [Performance Optimization](#performance-optimization)
- [Best Practices from Context7](#best-practices-from-context7)
- [Testing Strategy](#testing-strategy)
- [Migration Plan](#migration-plan)

---

## Overview

This document provides a comprehensive guide for implementing persistent storage of search performance metrics in the Private Blockchain system. Currently, metrics are stored only in memory and are lost when the JVM terminates, making them useless for CLI commands that run in separate JVM instances.

**Goal:** Persist search metrics to the database so they accumulate across CLI invocations and provide meaningful performance insights.

---

## Problem Statement

### Current Architecture Issues

❌ **Current Behavior:**
- `SearchMetrics` uses in-memory `ConcurrentHashMap` and `AtomicLong`
- Each CLI command runs in a **new JVM instance**
- Metrics are **reset to 0** on every command execution
- `search-metrics` command shows **no data** (always 0)

❌ **Why This Doesn't Work:**
```bash
# Command 1: Perform search (metrics recorded in memory)
java -jar blockchain-cli.jar search "test"
# JVM #1: totalSearches = 1

# Command 2: View metrics (NEW JVM, fresh metrics)
java -jar blockchain-cli.jar search-metrics
# JVM #2: totalSearches = 0  ❌ Lost!
```

✅ **Desired Behavior:**
- Metrics persist in database across CLI invocations
- Accumulate over time showing real usage patterns
- Enable performance analysis and optimization

---

## Solution Architecture

### High-Level Design

```
┌─────────────────────┐
│   SearchMetrics     │
│   (Service Layer)   │
└──────────┬──────────┘
           │
           │ 1. Load existing metrics from DB
           │ 2. Update in-memory for performance
           │ 3. Persist changes to DB (batch)
           │
           ▼
┌─────────────────────┐
│ SearchMetricsEntity │
│   (JPA Entity)      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   PostgreSQL/MySQL  │
│   H2/SQLite         │
└─────────────────────┘
```

### Key Design Decisions

1. **Hybrid Approach**: In-memory cache + DB persistence
   - Fast reads from memory during runtime
   - Periodic batch writes to DB (every N operations or time interval)
   - Load from DB on initialization

2. **Entity Per Search Type**: Separate row for each search type
   - `KEYWORD_SEARCH`, `CATEGORY_FILTER`, `FAST_INDEX`, etc.
   - Natural ID lookup by search type

3. **Optimistic Locking**: Handle concurrent updates
   - Use `@Version` for optimistic locking
   - Retry on version conflicts

---

## Entity Design

### SearchMetricsEntity

Based on **Context7 best practices** for entity design:

```java
package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import java.time.Instant;

/**
 * Persistent storage for search performance metrics.
 *
 * Design based on Context7 Hibernate best practices:
 * - Uses @NaturalId for efficient lookups by searchType
 * - Uses @Version for optimistic locking (concurrent updates)
 * - Composite statistics stored as @Embedded object
 * - Indexes on searchType for fast queries
 */
@Entity
@Table(
    name = "search_metrics",
    indexes = {
        @Index(name = "idx_search_type", columnList = "searchType"),
        @Index(name = "idx_last_updated", columnList = "lastUpdated")
    }
)
public class SearchMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Natural ID for this entity - the search type identifier.
     * Context7 Recommendation: Use @NaturalId for business-level unique attributes.
     * This enables efficient byNaturalId() lookups without loading by surrogate key.
     */
    @NaturalId
    @Column(nullable = false, unique = true, length = 100)
    private String searchType;

    /**
     * Optimistic locking version.
     * Context7 Recommendation: Use @Version for concurrent update handling.
     * Automatically managed by Hibernate - incremented on each update.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /**
     * Total number of searches performed for this type.
     */
    @Column(nullable = false)
    private Long totalSearches = 0L;

    /**
     * Number of searches that hit the cache.
     */
    @Column(nullable = false)
    private Long cacheHits = 0L;

    /**
     * Total search time in milliseconds.
     */
    @Column(nullable = false)
    private Long totalTimeMs = 0L;

    /**
     * Minimum search time observed.
     */
    @Column(nullable = false)
    private Long minTimeMs = Long.MAX_VALUE;

    /**
     * Maximum search time observed.
     */
    @Column(nullable = false)
    private Long maxTimeMs = 0L;

    /**
     * Total number of results returned across all searches.
     */
    @Column(nullable = false)
    private Long totalResults = 0L;

    /**
     * Timestamp of first metric record.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of last update.
     */
    @Column(nullable = false)
    private Instant lastUpdated;

    // ============================================
    // JPA Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastUpdated = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = Instant.now();
    }

    // ============================================
    // Business Methods
    // ============================================

    /**
     * Record a search operation.
     * Updates all relevant counters and statistics.
     */
    public void recordSearch(long durationMs, int results, boolean cacheHit) {
        this.totalSearches++;
        this.totalTimeMs += durationMs;
        this.totalResults += results;

        if (cacheHit) {
            this.cacheHits++;
        }

        // Update min/max times
        if (durationMs < this.minTimeMs) {
            this.minTimeMs = durationMs;
        }
        if (durationMs > this.maxTimeMs) {
            this.maxTimeMs = durationMs;
        }
    }

    /**
     * Calculate average search time.
     */
    public double getAverageTimeMs() {
        return totalSearches > 0 ? (double) totalTimeMs / totalSearches : 0.0;
    }

    /**
     * Calculate cache hit rate as percentage.
     */
    public double getCacheHitRate() {
        return totalSearches > 0 ? ((double) cacheHits / totalSearches) * 100.0 : 0.0;
    }

    /**
     * Calculate average results per search.
     */
    public double getAverageResults() {
        return totalSearches > 0 ? (double) totalResults / totalSearches : 0.0;
    }

    /**
     * Reset all metrics to zero.
     */
    public void reset() {
        this.totalSearches = 0L;
        this.cacheHits = 0L;
        this.totalTimeMs = 0L;
        this.minTimeMs = Long.MAX_VALUE;
        this.maxTimeMs = 0L;
        this.totalResults = 0L;
    }

    // ============================================
    // Getters and Setters
    // ============================================

    public Long getId() {
        return id;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public Long getVersion() {
        return version;
    }

    public Long getTotalSearches() {
        return totalSearches;
    }

    public Long getCacheHits() {
        return cacheHits;
    }

    public Long getTotalTimeMs() {
        return totalTimeMs;
    }

    public Long getMinTimeMs() {
        return minTimeMs == Long.MAX_VALUE ? 0L : minTimeMs;
    }

    public Long getMaxTimeMs() {
        return maxTimeMs;
    }

    public Long getTotalResults() {
        return totalResults;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    // ============================================
    // equals() and hashCode() based on Natural ID
    // Context7 Recommendation: Use natural ID for equality
    // ============================================

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SearchMetricsEntity)) return false;
        SearchMetricsEntity that = (SearchMetricsEntity) other;
        return searchType != null && searchType.equals(that.searchType);
    }

    @Override
    public int hashCode() {
        return searchType != null ? searchType.hashCode() : 0;
    }
}
```

### GlobalMetricsEntity (Optional)

For storing aggregated global statistics:

```java
@Entity
@Table(name = "global_search_metrics")
public class GlobalSearchMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Singleton pattern - only one row exists.
     * Use fixed ID = 1 for global metrics.
     */
    @Column(nullable = false, unique = true)
    private Long globalId = 1L;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long totalSearchesAllTypes = 0L;

    @Column(nullable = false)
    private Instant systemStartTime;

    @Column(nullable = false)
    private Instant lastSearchTime;

    // Getters, setters, business methods...
}
```

---

## Implementation Details

### 1. Create Repository

```java
package com.rbatllet.blockchain.repository;

import com.rbatllet.blockchain.entity.SearchMetricsEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SearchMetricsEntity operations.
 * Uses JPA EntityManager for database access.
 */
public class SearchMetricsRepository {

    private static final Logger logger = LoggerFactory.getLogger(SearchMetricsRepository.class);

    /**
     * Find metrics by search type using natural ID.
     * Context7 Recommendation: Use byNaturalId() for efficient lookups.
     */
    public Optional<SearchMetricsEntity> findBySearchType(EntityManager em, String searchType) {
        try {
            SearchMetricsEntity entity = em
                .unwrap(org.hibernate.Session.class)
                .byNaturalId(SearchMetricsEntity.class)
                .using("searchType", searchType)
                .load();
            return Optional.ofNullable(entity);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Find or create metrics entity for a search type.
     */
    public SearchMetricsEntity findOrCreate(EntityManager em, String searchType) {
        return findBySearchType(em, searchType).orElseGet(() -> {
            SearchMetricsEntity newEntity = new SearchMetricsEntity();
            newEntity.setSearchType(searchType);
            em.persist(newEntity);
            return newEntity;
        });
    }

    /**
     * Get all metrics ordered by total searches.
     */
    public List<SearchMetricsEntity> findAll(EntityManager em) {
        return em.createQuery(
            "SELECT m FROM SearchMetricsEntity m ORDER BY m.totalSearches DESC",
            SearchMetricsEntity.class
        ).getResultList();
    }

    /**
     * Reset all metrics (for testing or maintenance).
     */
    public int resetAll(EntityManager em) {
        return em.createQuery("DELETE FROM SearchMetricsEntity").executeUpdate();
    }
}
```

### 2. Modify SearchMetrics Service

```java
package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.SearchMetricsEntity;
import com.rbatllet.blockchain.repository.SearchMetricsRepository;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Search performance metrics with database persistence.
 *
 * Hybrid Architecture:
 * - In-memory cache for fast reads/writes during runtime
 * - Periodic batch persistence to database
 * - Load from database on initialization
 *
 * Context7 Best Practices Applied:
 * - Batch updates for performance (every 10 operations)
 * - Optimistic locking with retry on conflicts
 * - StatelessSession for bulk resets
 */
public class SearchMetrics {

    private static final Logger logger = LoggerFactory.getLogger(SearchMetrics.class);
    private static final int BATCH_PERSIST_THRESHOLD = 10; // Persist every N operations

    private final Map<String, PerformanceStats> inMemoryStats;
    private final SearchMetricsRepository repository;
    private final AtomicInteger operationsSincePersist;
    private volatile boolean loaded = false;

    public SearchMetrics() {
        this.inMemoryStats = new ConcurrentHashMap<>();
        this.repository = new SearchMetricsRepository();
        this.operationsSincePersist = new AtomicInteger(0);
        loadFromDatabase();
    }

    /**
     * Load existing metrics from database into memory.
     * Context7: Use regular Session for initial load.
     */
    private void loadFromDatabase() {
        if (loaded) return;

        JPAUtil.executeInTransaction(em -> {
            List<SearchMetricsEntity> entities = repository.findAll(em);

            for (SearchMetricsEntity entity : entities) {
                PerformanceStats stats = new PerformanceStats();
                stats.loadFrom(entity);
                inMemoryStats.put(entity.getSearchType(), stats);
            }

            logger.info("📊 Loaded {} search metric types from database", entities.size());
            loaded = true;
        });
    }

    /**
     * Record a search operation.
     * Context7 Best Practice: Batch updates for performance.
     */
    public void recordSearch(String searchType, long durationMs, int results, boolean cacheHit) {
        // Update in-memory stats (fast)
        PerformanceStats stats = inMemoryStats.computeIfAbsent(
            searchType,
            k -> new PerformanceStats()
        );
        stats.recordSearch(durationMs, results, cacheHit);

        // Persist to DB in batches (Context7: batch_size optimization)
        if (operationsSincePersist.incrementAndGet() >= BATCH_PERSIST_THRESHOLD) {
            persistToDatabaseBatch();
        }
    }

    /**
     * Persist all in-memory metrics to database.
     * Context7 Best Practice: Use flush() and clear() for batch operations.
     */
    private void persistToDatabaseBatch() {
        operationsSincePersist.set(0);

        JPAUtil.executeInTransaction(em -> {
            int count = 0;

            for (Map.Entry<String, PerformanceStats> entry : inMemoryStats.entrySet()) {
                String searchType = entry.getKey();
                PerformanceStats stats = entry.getValue();

                // Find or create entity
                SearchMetricsEntity entity = repository.findOrCreate(em, searchType);

                // Update entity from stats
                stats.applyTo(entity);

                // Context7: Flush and clear every 20 entities for memory management
                if (++count % 20 == 0) {
                    em.flush();
                    em.clear();
                }
            }

            logger.debug("📊 Persisted {} search metric types to database", count);
        }, maxRetries = 3); // Retry on optimistic lock exceptions
    }

    /**
     * Force immediate persistence to database.
     * Call this on application shutdown or before critical operations.
     */
    public void flush() {
        persistToDatabaseBatch();
    }

    /**
     * Reset all metrics.
     * Context7 Best Practice: Use StatelessSession for bulk deletes.
     */
    public void reset() {
        // Clear in-memory
        inMemoryStats.clear();
        operationsSincePersist.set(0);

        // Delete from database using StatelessSession for performance
        JPAUtil.executeInStatelessSession(session -> {
            session.beginTransaction();

            // Bulk delete
            int deleted = session.createMutationQuery(
                "DELETE FROM SearchMetricsEntity"
            ).executeUpdate();

            session.getTransaction().commit();

            logger.info("📊 Reset {} search metric types", deleted);
        });
    }

    /**
     * Get statistics for a specific search type.
     */
    public PerformanceStats getStats(String searchType) {
        return inMemoryStats.get(searchType);
    }

    /**
     * Get all statistics.
     */
    public Map<String, PerformanceStats> getAllStats() {
        return new ConcurrentHashMap<>(inMemoryStats);
    }

    // ============================================
    // Inner Class: PerformanceStats
    // ============================================

    public static class PerformanceStats {
        private final AtomicLong searches = new AtomicLong(0);
        private final AtomicLong cacheHits = new AtomicLong(0);
        private final AtomicLong totalTimeMs = new AtomicLong(0);
        private volatile long minTimeMs = Long.MAX_VALUE;
        private volatile long maxTimeMs = 0;
        private final AtomicLong totalResults = new AtomicLong(0);

        public void recordSearch(long durationMs, int results, boolean cacheHit) {
            searches.incrementAndGet();
            totalTimeMs.addAndGet(durationMs);
            totalResults.addAndGet(results);

            if (cacheHit) {
                cacheHits.incrementAndGet();
            }

            // Update min/max atomically
            synchronized (this) {
                if (durationMs < minTimeMs) minTimeMs = durationMs;
                if (durationMs > maxTimeMs) maxTimeMs = durationMs;
            }
        }

        /**
         * Load stats from entity.
         */
        public void loadFrom(SearchMetricsEntity entity) {
            searches.set(entity.getTotalSearches());
            cacheHits.set(entity.getCacheHits());
            totalTimeMs.set(entity.getTotalTimeMs());
            minTimeMs = entity.getMinTimeMs();
            maxTimeMs = entity.getMaxTimeMs();
            totalResults.set(entity.getTotalResults());
        }

        /**
         * Apply stats to entity.
         */
        public void applyTo(SearchMetricsEntity entity) {
            entity.recordSearch(
                totalTimeMs.get() - (entity.getTotalTimeMs()),
                (int)(totalResults.get() - entity.getTotalResults()),
                cacheHits.get() > entity.getCacheHits()
            );
        }

        // Getters...
        public long getSearches() { return searches.get(); }
        public double getCacheHitRate() {
            long total = searches.get();
            return total > 0 ? ((double) cacheHits.get() / total) * 100.0 : 0.0;
        }
        public double getAverageTimeMs() {
            long total = searches.get();
            return total > 0 ? (double) totalTimeMs.get() / total : 0.0;
        }
        // ... more getters
    }
}
```

---

## Performance Optimization

### 1. Batch Configuration

Based on **Context7 recommendations**, configure these properties:

```properties
# application.properties or persistence.xml

# Context7 Best Practice: Enable JDBC batching
spring.jpa.properties.hibernate.jdbc.batch_size=25
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Context7: Enable batch for versioned entities (@Version)
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true

# For MySQL: Enable statement rewriting
spring.datasource.url=jdbc:mysql://localhost:3306/blockchain?rewriteBatchedStatements=true&cachePrepStmts=true&useServerPrepStmts=true

# Enable statistics for monitoring
spring.jpa.properties.hibernate.generate_statistics=true

# Disable second-level cache for metrics (frequent updates)
spring.jpa.properties.hibernate.cache.use_second_level_cache=false
```

### 2. Indexing Strategy

```sql
-- Automatically created by @Index annotations
CREATE INDEX idx_search_type ON search_metrics(searchType);
CREATE INDEX idx_last_updated ON search_metrics(lastUpdated);

-- For analytics queries
CREATE INDEX idx_total_searches ON search_metrics(totalSearches DESC);
```

### 3. Memory Management

Context7 Recommendations:
- Use `flush()` and `clear()` every 20-50 entities in batch operations
- Use `StatelessSession` for bulk deletes/resets
- Detach entities after processing to free memory

---

## Best Practices from Context7

### ✅ Applied in This Design

1. **Optimistic Locking with @Version**
   - Source: hibernate-orm/Locking.adoc
   - Prevents lost updates in concurrent scenarios
   - Automatic version increments by Hibernate

2. **Natural ID for Business Keys**
   - Source: hibernate-orm/Entities.adoc
   - `searchType` as natural identifier
   - Efficient `byNaturalId()` lookups
   - Better equals()/hashCode() implementation

3. **Batch Operations**
   - Source: hibernate-springboot/README.md
   - `jdbc.batch_size=25` for optimal throughput
   - `order_inserts=true` for related entities
   - MySQL-specific optimizations

4. **StatelessSession for Bulk Ops**
   - Source: hibernate-orm/Batching.adoc
   - Used in `reset()` method
   - Bypasses first-level cache
   - Better performance for bulk deletes

5. **Session Flush and Clear**
   - Source: hibernate-orm/Batching.adoc
   - Periodic `flush()` every 20 entities
   - `clear()` to free memory
   - Prevents OutOfMemoryError

### 📚 Context7 References

- **Hibernate ORM**: `/hibernate/hibernate-orm`
  - Entity design patterns
  - Locking strategies
  - Performance tuning

- **Hibernate Spring Boot**: `/anghelleonard/hibernate-springboot`
  - Batch operation best practices
  - MySQL optimizations
  - Real-world examples

---

## Testing Strategy

### 1. Unit Tests

```java
@Test
void testMetricsPersistence() {
    SearchMetrics metrics = new SearchMetrics();

    // Record searches
    metrics.recordSearch("KEYWORD_SEARCH", 50, 10, false);
    metrics.recordSearch("KEYWORD_SEARCH", 75, 5, true);

    // Force persistence
    metrics.flush();

    // Create new instance (simulates CLI restart)
    SearchMetrics newMetrics = new SearchMetrics();

    // Verify metrics were loaded from DB
    PerformanceStats stats = newMetrics.getStats("KEYWORD_SEARCH");
    assertEquals(2, stats.getSearches());
    assertEquals(50.0, stats.getCacheHitRate(), 0.1);
}
```

### 2. Concurrent Update Test

```java
@Test
void testOptimisticLocking() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(100);

    for (int i = 0; i < 100; i++) {
        executor.submit(() -> {
            try {
                metrics.recordSearch("CONCURRENT_TEST", 10, 1, false);
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    metrics.flush();

    // Verify all 100 searches were recorded
    assertEquals(100, metrics.getStats("CONCURRENT_TEST").getSearches());
}
```

### 3. CLI Integration Test

```bash
#!/usr/bin/env zsh
# Test metrics persistence across CLI invocations

# Search 1
java -jar blockchain-cli.jar search "test"

# Search 2
java -jar blockchain-cli.jar search "blockchain"

# View metrics (should show 2 searches)
java -jar blockchain-cli.jar search-metrics

# Expected output:
# Total Searches: 2
# Average Time: X ms
# ...
```

---

## Migration Plan

### Phase 1: Entity and Repository (Week 1)

1. Create `SearchMetricsEntity`
2. Create `SearchMetricsRepository`
3. Add database migration scripts
4. Unit tests for entity and repository

### Phase 2: Service Integration (Week 2)

1. Modify `SearchMetrics` to use hybrid approach
2. Implement batch persistence logic
3. Add optimistic locking retry logic
4. Integration tests

### Phase 3: CLI Testing (Week 3)

1. Test metrics across CLI invocations
2. Performance testing with high load
3. Verify batch operations work correctly
4. Update documentation

### Phase 4: Production Rollout (Week 4)

1. Database migration on production
2. Monitor performance metrics
3. Tune batch sizes if needed
4. Enable metrics dashboard

---

## Database Migration

### H2/SQLite (Development)

```sql
CREATE TABLE search_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    search_type VARCHAR(100) NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    total_searches BIGINT NOT NULL DEFAULT 0,
    cache_hits BIGINT NOT NULL DEFAULT 0,
    total_time_ms BIGINT NOT NULL DEFAULT 0,
    min_time_ms BIGINT NOT NULL DEFAULT 9223372036854775807,
    max_time_ms BIGINT NOT NULL DEFAULT 0,
    total_results BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL
);

CREATE INDEX idx_search_type ON search_metrics(search_type);
CREATE INDEX idx_last_updated ON search_metrics(last_updated);
```

### PostgreSQL (Production)

```sql
CREATE TABLE search_metrics (
    id BIGSERIAL PRIMARY KEY,
    search_type VARCHAR(100) NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    total_searches BIGINT NOT NULL DEFAULT 0,
    cache_hits BIGINT NOT NULL DEFAULT 0,
    total_time_ms BIGINT NOT NULL DEFAULT 0,
    min_time_ms BIGINT NOT NULL DEFAULT 9223372036854775807,
    max_time_ms BIGINT NOT NULL DEFAULT 0,
    total_results BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_search_type ON search_metrics(search_type);
CREATE INDEX idx_last_updated ON search_metrics(last_updated);
CREATE INDEX idx_total_searches ON search_metrics(total_searches DESC);
```

---

## Conclusion

This implementation guide provides a complete solution for persisting search metrics using Hibernate/JPA best practices from Context7. The hybrid in-memory + database approach balances performance with persistence, making the `search-metrics` CLI command actually useful.

### Key Takeaways

✅ Metrics persist across CLI invocations
✅ Optimistic locking prevents lost updates
✅ Batch operations optimize database performance
✅ Natural ID enables efficient lookups
✅ StatelessSession for bulk operations
✅ Production-ready with proper indexing and monitoring

### Next Steps

1. Review this design with the team
2. Create implementation tasks
3. Begin Phase 1 (Entity and Repository)
4. Iterate based on testing feedback

---

**Document Version**: 1.0
**Created**: 2026-01-12
**Author**: Development Team
**Context7 Consultation**: ✅ Completed
