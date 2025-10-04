# Data Management

Guides for managing blockchain data efficiently: pagination, batching, metadata, and block operations.

## 📚 Documents (6 files)

### Pagination & Batching
| Document | Description |
|----------|-------------|
| **[PAGINATION_AND_BATCH_PROCESSING_GUIDE.md](PAGINATION_AND_BATCH_PROCESSING_GUIDE.md)** | Memory-efficient data access patterns |
| **[FILTERED_PAGINATION_API.md](FILTERED_PAGINATION_API.md)** | Filtered pagination patterns |
| **[BATCH_OPTIMIZATION_GUIDE.md](BATCH_OPTIMIZATION_GUIDE.md)** | Batch processing optimization |

### Metadata & Block Management
| Document | Description |
|----------|-------------|
| **[METADATA_MANAGEMENT_GUIDE.md](METADATA_MANAGEMENT_GUIDE.md)** | Dynamic metadata updates |
| **[BLOCK_NUMBER_DECRYPTION_EXAMPLES.md](BLOCK_NUMBER_DECRYPTION_EXAMPLES.md)** | Block number decryption patterns |
| **[BLOCK_NUMBER_DECRYPTION_METHODS.md](BLOCK_NUMBER_DECRYPTION_METHODS.md)** | Block number decryption reference |

## ⚠️ Memory Safety

**Never load all blocks into memory!** Always use pagination:
- `getBlocksPaginated(offset, limit)` - Max 10K per batch
- `processChainInBatches(consumer, batchSize)` - Stream processing

---
**Directory**: `docs/data-management/` | **Files**: 6 | **Updated**: 2025-10-04
