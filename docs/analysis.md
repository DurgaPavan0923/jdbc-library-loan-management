# Analysis: Transaction Behavior & Performance Findings

**Library Loan Management System — Apache Derby JDBC**

---

## 1. Transaction Management & Data Integrity

### How Transaction Boundaries Preserve Integrity During Failure

The `processLoan()` operation updates three tables — `Books`, `Loans`, and `Members` — in a single logical unit. Without an explicit transaction, a JVM crash or SQLException after step 2 (book marked unavailable) but before step 4 (loan count incremented) would leave the database in a **partial state**: the book appears checked out, but no loan record exists and the member's count is stale.

By wrapping all three updates inside `setAutoCommit(false)` / `commit()` / `rollback()`, Derby's **write-ahead log (WAL)** guarantees that either all changes are flushed to disk together on commit, or none are — the UNDO log entries are replayed on rollback, restoring every modified page to its pre-transaction state.

#### Savepoint-Based Partial Rollback

The implementation adds two savepoints:

```
sp1  ← set after Books update
sp2  ← set after Loans insert
```

If step 4 (`Members` update) fails:
1. `rollback(sp2)` — undoes the Loans insert.
2. `rollback(sp1)` — undoes the Books availability update.
3. `rollback()` — ensures a clean slate.

This gives granular recovery visibility while still guaranteeing atomicity. In a real system, savepoints enable retry logic (e.g., retry only the last step) without replaying expensive earlier steps.

#### Constraint Violations & Consistency

Derby enforces:
- **UNIQUE** on `Books.ISBN` and `Members.Email` — prevents duplicate catalog entries.
- **FOREIGN KEY** (`Loans.BookID → Books.BookID`, `Loans.MemberID → Members.MemberID`) — orphaned loan records are impossible.
- **CHECK** constraints (`Available IN (0,1)`, `ActiveLoanCount >= 0`, `Status IN (...)`) — domain rules enforced at the storage layer, not only in application code.

When any constraint fires, Derby throws `SQLException` with a specific `SQLState` code (e.g., `23505` for UNIQUE violation). The `try-catch` block in `TransactionService` catches this and calls `rollback()`, so the partial write is undone and the data returns to a consistent state. This was demonstrated by attempting a duplicate ISBN insert — the rollback left the catalog unchanged.

---

## 2. Why Certain JDBC Patterns Outperform Others

### Batch Insert vs. Individual Inserts

| Metric | Individual (1K rows) | Batch (1K rows) | Speedup |
|--------|----------------------|-----------------|---------|
| Round-trips to Derby | 1,000 | ~2 (batches of 500) | ~500× fewer |
| Log flush events | 1 (single transaction) | 1 (single transaction) | Equal |
| Typical speedup | — | **3–8×** | Varies by JIT warmup |

The dominant cost in individual inserts is **protocol overhead**: each `executeUpdate()` call parses the SQL, acquires a lock on the heap page, writes a WAL entry, and releases. With `addBatch()/executeBatch()`, Derby pipelines these operations — the SQL is parsed once (PreparedStatement reuse), lock acquisition is batched, and WAL entries are written in a single sequential pass. The speedup grows super-linearly at 10K rows because Derby's buffer pool stays warm across the entire batch.

### Indexed Lookup vs. Full-Table Scan

Derby uses a **B-tree** index for `Loans.MemberID`. A lookup by `MemberID=1` traverses ~2–3 B-tree levels regardless of table size, compared to a sequential scan that reads every page in `Loans`. At 500 rows the difference is modest (single-digit ms); at millions of rows the indexed path stays O(log n) while the scan grows linearly.

The `ReturnDate` index is particularly valuable for overdue queries: `WHERE DueDate < CURRENT_DATE AND Status='ACTIVE'` can use the B-tree to skip all future-dated entries without scanning them.

### Statement vs. PreparedStatement

`Statement` re-parses and re-compiles the query plan on every `execute()` call. `PreparedStatement` compiles once, caches the plan in Derby's **statement cache**, and only substitutes parameter values on subsequent calls. At 200 operations the PreparedStatement path is typically **1.3–2× faster** and eliminates SQL injection risk (parameter binding prevents string interpolation attacks).

For Derby specifically, the plan cache is bounded by `derby.language.statementCacheSize` (default 100). Long-lived applications benefit from keeping a small set of PreparedStatements open for hot paths.

### Transaction Granularity

| Approach | Commits | Log Flushes | Throughput |
|----------|---------|-------------|------------|
| Per-op (autoCommit=true) | 100 | 100 | Low |
| Batched (1 transaction) | 1 | 1 | High |

Every `commit()` in Derby forces the WAL buffer to be **synchronously flushed to disk** (fsync). With 100 per-operation commits, this happens 100 times. With a single batched commit it happens once. The throughput difference is typically **5–15×**, bounded by disk I/O latency.

The trade-off: a batched transaction is an **all-or-nothing** unit — a failure midway loses all 100 operations. Per-operation commits give maximum durability but sacrifice throughput. Production systems typically group logically related operations (e.g., a single user request) into one transaction and use application-level retry for failures.

---

## 3. Trade-Offs: Safety vs. Raw Speed

| Technique | Safety Benefit | Speed Cost |
|-----------|---------------|------------|
| Explicit transactions | Atomicity, rollback on failure | ~5–15% overhead vs. autoCommit |
| PreparedStatement | SQL injection prevention, plan reuse | Marginally slower for one-off queries |
| Batch insert | — (same atomicity if inside transaction) | Up to 8× faster for bulk loads |
| Indexed columns | Query correctness not affected | ~5% insert overhead per index |
| CHECK/FK constraints | Data integrity enforced at DB layer | ~1–3% write overhead |

**Recommended defaults for this system:**
- Always use `PreparedStatement` — safety and speed together.
- Wrap multi-step business operations in explicit transactions — correctness first.
- Use `addBatch()/executeBatch()` for any bulk data operation (seeding, reports).
- Keep transactions short — long-running transactions hold locks and block concurrent readers (even under READ_COMMITTED, write locks are held until commit).

---

## 4. Derby-Specific Notes

- **Embedded vs. Network**: Embedded Derby avoids network serialization overhead, making individual operations faster than client/server databases. The trade-off is single-JVM access — only one JVM can open the embedded database at a time.
- **Buffer Pool**: Derby's page cache (configurable via `derby.storage.pageCacheSize`) dramatically affects benchmark repeatability. The warm-up phase in `PerformanceEvaluator` ensures the buffer pool is populated before timing begins.
- **WAL Checkpointing**: Derby checkpoints periodically, writing dirty pages to disk. Long benchmarks may show a periodic latency spike when a checkpoint occurs during timing. The standard deviation column in the report captures this variability.
