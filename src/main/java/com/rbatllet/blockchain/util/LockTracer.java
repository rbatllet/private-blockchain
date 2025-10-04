package com.rbatllet.blockchain.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.StampedLock;

/**
 * Wrapper around StampedLock that logs all lock acquisitions and releases
 * for debugging deadlock issues.
 * 
 * This class adds minimal overhead logging to track lock behavior without
 * expensive operations like getStackTrace().
 */
public class LockTracer {
    private static final Logger logger = LoggerFactory.getLogger(LockTracer.class);
    
    private final StampedLock lock;
    private final String lockName;
    
    public LockTracer(StampedLock lock, String lockName) {
        this.lock = lock;
        this.lockName = lockName;
    }
    
    /**
     * Acquire read lock with logging
     */
    public long readLock() {
        String threadName = Thread.currentThread().getName();
        logger.debug("🔒 [{}] ACQUIRING readLock on {}", threadName, lockName);
        
        long stamp = lock.readLock();
        
        logger.debug("✅ [{}] ACQUIRED readLock on {} (stamp={})", threadName, lockName, stamp);
        return stamp;
    }
    
    /**
     * Release read lock with logging
     */
    public void unlockRead(long stamp) {
        String threadName = Thread.currentThread().getName();
        logger.debug("🔓 [{}] RELEASING readLock on {} (stamp={})", threadName, lockName, stamp);
        
        lock.unlockRead(stamp);
        
        logger.debug("✅ [{}] RELEASED readLock on {} (stamp={})", threadName, lockName, stamp);
    }
    
    /**
     * Acquire write lock with logging
     */
    public long writeLock() {
        String threadName = Thread.currentThread().getName();
        logger.debug("🔒 [{}] ACQUIRING writeLock on {}", threadName, lockName);
        
        long stamp = lock.writeLock();
        
        logger.debug("✅ [{}] ACQUIRED writeLock on {} (stamp={})", threadName, lockName, stamp);
        return stamp;
    }
    
    /**
     * Release write lock with logging
     */
    public void unlockWrite(long stamp) {
        String threadName = Thread.currentThread().getName();
        logger.debug("🔓 [{}] RELEASING writeLock on {} (stamp={})", threadName, lockName, stamp);
        
        lock.unlockWrite(stamp);
        
        logger.debug("✅ [{}] RELEASED writeLock on {} (stamp={})", threadName, lockName, stamp);
    }
    
    /**
     * Try optimistic read (no logging as it doesn't block)
     */
    public long tryOptimisticRead() {
        return lock.tryOptimisticRead();
    }
    
    /**
     * Validate optimistic read
     */
    public boolean validate(long stamp) {
        return lock.validate(stamp);
    }
    
    /**
     * Get the underlying StampedLock (for migration purposes)
     */
    public StampedLock getUnderlyingLock() {
        return lock;
    }
}
