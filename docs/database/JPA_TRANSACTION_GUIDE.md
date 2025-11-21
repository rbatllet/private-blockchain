# JPA Transaction Isolation and getLastBlock() Guide

**⚠️ Version 1.0.6 Critical Fix: Transaction-Aware Block Retrieval**

A subtle but critical bug was discovered in v1.0.6 related to JPA transaction isolation. This guide documents the issue, solution, and best practices.

## The Problem: EntityManager Transaction Isolation

**Symptom:** `ConstraintViolationException` with duplicate `block_number` values when creating sequential blocks within a transaction.

**Root Cause:** The method `BlockRepository.getLastBlock()` (no parameters) creates a **new EntityManager** to query the database. Under JPA's READ COMMITTED isolation level, this new EntityManager **cannot see uncommitted data** from the current transaction. This leads to stale reads:

```java
// ❌ BUG - Stale read within transaction
@Transactional
public void createMultipleBlocks() {
    Block block1 = new Block();
    block1.setBlockNumber(blockRepository.getLastBlock().getBlockNumber() + 1);  // Gets 3
    em.persist(block1);  // Saves block #3 (uncommitted)

    Block block2 = new Block();
    block2.setBlockNumber(blockRepository.getLastBlock().getBlockNumber() + 1);  // Gets 3 again! (stale read)
    em.persist(block2);  // ConstraintViolationException: duplicate block_number=3
}
```

**Why This Happens:**
1. `getLastBlock()` creates a new `EntityManager` outside the transaction
2. New EM uses separate persistence context (cannot see uncommitted block #3)
3. Returns last committed block (block #2)
4. Both blocks calculate same block number (3)
5. Database constraint violation on second persist

## The Solution: Transaction-Aware Method

**v1.0.6 introduces `BlockRepository.getLastBlock(EntityManager em)`** - accepts the current transaction's EntityManager:

```java
// ✅ CORRECT - Transaction-aware read
@Transactional
public void createMultipleBlocks() {
    EntityManager em = // ... from @PersistenceContext or method parameter

    Block block1 = new Block();
    block1.setBlockNumber(blockRepository.getLastBlock(em).getBlockNumber() + 1);  // Gets 3
    em.persist(block1);  // Saves block #3 (uncommitted but visible to em)

    Block block2 = new Block();
    block2.setBlockNumber(blockRepository.getLastBlock(em).getBlockNumber() + 1);  // Gets 4 (sees block #3)
    em.persist(block2);  // Success! block_number=4
}
```

**Performance Benefit:** The new method uses `MAX(b.blockNumber)` instead of `ORDER BY b.blockNumber DESC`, providing O(1) indexed query performance vs O(n log n) sorting.

## When to Use Each Method

### Use `getLastBlock()` (no parameters) for:
- ✅ Read-only operations outside transactions
- ✅ Test setup and verification
- ✅ Public API methods that don't modify state
- ✅ Reporting and statistics
- ✅ Checkpoint creation (reads committed state)

### Use `getLastBlock(EntityManager em)` for:
- ✅ Inside active JPA transactions
- ✅ When creating new blocks
- ✅ When block number calculation must see uncommitted data
- ✅ Internal blockchain operations (addBlock, rollback, etc.)
- ✅ Any operation that needs transaction-consistent reads

## Code Patterns

```java
// Pattern 1: Read-only access (no transaction)
public long getChainLength() {
    Block lastBlock = blockRepository.getLastBlock();  // ✅ OK - read-only
    return lastBlock.getBlockNumber() + 1;
}

// Pattern 2: Within transaction - WRONG
@Transactional
public void addNewBlock(Block block) {
    Block last = blockRepository.getLastBlock();  // ❌ WRONG - stale read!
    block.setBlockNumber(last.getBlockNumber() + 1);
    em.persist(block);
}

// Pattern 3: Within transaction - CORRECT
@Transactional
public void addNewBlock(Block block, EntityManager em) {
    Block last = blockRepository.getLastBlock(em);  // ✅ CORRECT - transaction-aware
    block.setBlockNumber(last.getBlockNumber() + 1);
    em.persist(block);
}

// Pattern 4: Multiple blocks in loop - CRITICAL
@Transactional
public void addMultipleBlocks(List<String> data, EntityManager em) {
    for (String content : data) {
        Block block = new Block();
        block.setBlockNumber(blockRepository.getLastBlock(em).getBlockNumber() + 1);  // ✅ Must use em version!
        block.setData(content);
        em.persist(block);
        // Without em parameter, all blocks would get same number!
    }
}
```

## Migration Checklist

When working with blockchain operations inside transactions:

1. ✅ Check if method is annotated with `@Transactional` or uses `EntityManager.getTransaction()`
2. ✅ If yes, ensure `getLastBlock(em)` is used, not `getLastBlock()`
3. ✅ Pass EntityManager through method signatures when needed
4. ✅ For off-chain operations using `addBlockWithOffChainData()`, verify it uses transaction-aware version internally
5. ✅ Review loops that create multiple blocks - these are highest risk for stale reads

## Additional Resources

- 📖 **Technical Documentation**: [TRANSACTION_ISOLATION_FIX.md](TRANSACTION_ISOLATION_FIX.md)
- 📖 **API Guide**: [../reference/API_GUIDE.md](../reference/API_GUIDE.md) - "Transaction-Aware Block Access" section
- 📖 **Javadoc**: Enhanced documentation on both `getLastBlock()` variants with usage examples
- 📖 **Documentation Summary**: [GETLASTBLOCK_DOCUMENTATION_UPDATE.md](GETLASTBLOCK_DOCUMENTATION_UPDATE.md)

## Version History

- **v1.0.6** (2025-01-09): Added `getLastBlock(EntityManager em)`, fixed `Blockchain.addBlockWithOffChainData()`, comprehensive documentation
- **Pre-v1.0.6**: Only `getLastBlock()` available (transaction isolation bug present)
