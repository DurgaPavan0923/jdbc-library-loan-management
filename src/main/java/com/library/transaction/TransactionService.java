package com.library.transaction;

import com.library.connection.ConnectionManager;

import java.sql.*;

/**
 * Provides explicit transaction boundary management.
 *
 * Demonstrates:
 *  - setAutoCommit(false) / commit() / rollback()
 *  - Savepoints for partial rollback
 *  - Isolation-level demonstration
 *  - ACID property enforcement
 */
public class TransactionService {

    private final ConnectionManager connectionManager;

    public TransactionService(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // -------------------------------------------------------------------------
    // Core Loan Transaction  (main Phase 2 demonstration)
    // -------------------------------------------------------------------------

    /**
     * Processes a book loan as a single ACID transaction:
     *  Step 1 - Verify book availability
     *  Step 2 - Update Books.Available = 0           [SAVEPOINT sp1]
     *  Step 3 - Insert Loans record                  [SAVEPOINT sp2]
     *  Step 4 - Increment Members.ActiveLoanCount
     *
     * If step 4 fails, we roll back to sp2 (undo loan insert) then to sp1 (undo book update).
     * Any earlier failure rolls back the entire transaction.
     */
    public int processLoan(int bookId, int memberId) throws SQLException {
        System.out.println("\n[TransactionService] Starting processLoan(bookId=" + bookId
                + ", memberId=" + memberId + ")");

        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false); // ← Explicit transaction control

            Savepoint sp1 = null;
            Savepoint sp2 = null;

            try {
                // ── Step 1: Verify book availability ────────────────────────
                if (!isBookAvailable(conn, bookId)) {
                    conn.rollback();
                    throw new SQLException("Book " + bookId + " is not available for loan.");
                }
                System.out.println("  [Step 1] Book availability confirmed.");

                // ── Step 2: Mark book as unavailable ────────────────────────
                sp1 = conn.setSavepoint("sp_book_update");
                updateBookAvailability(conn, bookId, false);
                System.out.println("  [Step 2] Book marked unavailable. Savepoint sp1 set.");

                // ── Step 3: Insert loan record ───────────────────────────────
                sp2 = conn.setSavepoint("sp_loan_insert");
                int loanId = insertLoanRecord(conn, bookId, memberId);
                System.out.println("  [Step 3] Loan record inserted (loanId=" + loanId + "). Savepoint sp2 set.");

                // ── Step 4: Update member's active loan count ────────────────
                updateMemberLoanCount(conn, memberId, +1);
                System.out.println("  [Step 4] Member active loan count incremented.");

                conn.commit(); // ← All steps succeeded
                System.out.println("  [COMMIT] Transaction committed successfully.");
                return loanId;

            } catch (SQLException e) {
                System.err.println("  [ERROR] " + e.getMessage());
                attemptPartialRollback(conn, sp1, sp2, e);
                throw e;
            }
        }
    }

    /**
     * Processes a book return as an ACID transaction:
     *  - Sets Loans.ReturnDate and Status = 'RETURNED'
     *  - Marks Books.Available = 1
     *  - Decrements Members.ActiveLoanCount
     */
    public void processReturn(int loanId) throws SQLException {
        System.out.println("\n[TransactionService] Processing return for loanId=" + loanId);

        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Fetch loan details
                int[] ids = getLoanDetails(conn, loanId);
                if (ids == null) throw new SQLException("Loan not found: " + loanId);

                int bookId   = ids[0];
                int memberId = ids[1];
                String status = getLoanStatus(conn, loanId);

                if ("RETURNED".equals(status)) {
                    throw new SQLException("Loan " + loanId + " is already returned.");
                }

                // Update loan record
                String updateLoan =
                    "UPDATE Loans SET ReturnDate = CURRENT_DATE, Status = 'RETURNED' WHERE LoanID = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateLoan)) {
                    ps.setInt(1, loanId);
                    ps.executeUpdate();
                }

                // Restore book availability
                updateBookAvailability(conn, bookId, true);

                // Decrement member active loan count
                updateMemberLoanCount(conn, memberId, -1);

                conn.commit();
                System.out.println("  [COMMIT] Return processed successfully.");

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("  [ROLLBACK] Return failed: " + e.getMessage());
                throw e;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Savepoint partial-rollback helper
    // -------------------------------------------------------------------------

    private void attemptPartialRollback(Connection conn, Savepoint sp1, Savepoint sp2,
                                        SQLException cause) {
        try {
            if (sp2 != null) {
                conn.rollback(sp2);
                System.err.println("  [PARTIAL ROLLBACK] Rolled back to sp2 (loan insert undone).");
                // also undo book update
                if (sp1 != null) {
                    conn.rollback(sp1);
                    System.err.println("  [PARTIAL ROLLBACK] Rolled back to sp1 (book update undone).");
                }
            }
            conn.rollback(); // ensure complete rollback
            System.err.println("  [ROLLBACK] Full rollback complete.");
        } catch (SQLException rbEx) {
            System.err.println("  [ROLLBACK ERROR] " + rbEx.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Isolation Level Demonstration
    // -------------------------------------------------------------------------

    /**
     * Demonstrates transaction isolation by reading book availability at
     * READ_COMMITTED (default Derby isolation).
     */
    public void demonstrateIsolation(int bookId) throws SQLException {
        System.out.println("\n[TransactionService] Isolation demonstration for bookId=" + bookId);

        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT BookID, Title, Available FROM Books WHERE BookID = ?")) {
                ps.setInt(1, bookId);

                // First read
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.printf("  [T1 Read 1] BookID=%d, Title=%s, Available=%d%n",
                                rs.getInt(1), rs.getString(2), rs.getInt(3));
                    }
                }

                // Simulate concurrent update by another connection
                simulateConcurrentUpdate(bookId);

                // Second read – under READ_COMMITTED this may see the new value
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.printf("  [T1 Read 2] BookID=%d, Title=%s, Available=%d  "
                                + "(may differ due to READ_COMMITTED)%n",
                                rs.getInt(1), rs.getString(2), rs.getInt(3));
                    }
                }

                conn.commit();
            }
        }
    }

    private void simulateConcurrentUpdate(int bookId) throws SQLException {
        // Uses a separate connection to simulate another session's commit
        try (Connection conn2 = connectionManager.getConnection()) {
            conn2.setAutoCommit(true);
            try (PreparedStatement ps = conn2.prepareStatement(
                    "UPDATE Books SET Available = 0 WHERE BookID = ? AND Available = 1")) {
                ps.setInt(1, bookId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("  [T2 Commit] Simulated concurrent session marked book unavailable.");
                    // Restore for demo cleanliness
                    try (PreparedStatement restore = conn2.prepareStatement(
                            "UPDATE Books SET Available = 1 WHERE BookID = ?")) {
                        restore.setInt(1, bookId);
                        restore.executeUpdate();
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Constraint Violation Demonstration
    // -------------------------------------------------------------------------

    /**
     * Attempts to insert a duplicate ISBN to demonstrate constraint violation
     * and verify that rollback preserves data consistency.
     */
    public void demonstrateConstraintViolation(String duplicateIsbn) {
        System.out.println("\n[TransactionService] Demonstrating constraint violation (ISBN: "
                + duplicateIsbn + ")");

        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Books (ISBN, Title, Author, Genre, PublishYear) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, duplicateIsbn);
                ps.setString(2, "Duplicate Book");
                ps.setString(3, "Test Author");
                ps.setString(4, "Test");
                ps.setInt(5, 2024);
                ps.executeUpdate();

                conn.commit(); // Should not reach here
                System.out.println("  ERROR: Constraint not enforced (should not happen)!");

            } catch (SQLException e) {
                conn.rollback();
                System.out.println("  [Expected] Constraint violation caught: " + e.getMessage());
                System.out.println("  [Rollback] Data consistency preserved. SQLState: " + e.getSQLState());
            }
        } catch (SQLException e) {
            System.err.println("[demonstrateConstraintViolation] Connection error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // SQL Helpers (reusable within transactions)
    // -------------------------------------------------------------------------

    private boolean isBookAvailable(Connection conn, int bookId) throws SQLException {
        String sql = "SELECT Available FROM Books WHERE BookID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private void updateBookAvailability(Connection conn, int bookId, boolean available)
            throws SQLException {
        String sql = "UPDATE Books SET Available = ? WHERE BookID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setInt(2, bookId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Book not found: " + bookId);
        }
    }

    private int insertLoanRecord(Connection conn, int bookId, int memberId) throws SQLException {
        String sql = """
            INSERT INTO Loans (BookID, MemberID, LoanDate, DueDate, Status)
            VALUES (?, ?, CURRENT_DATE, {fn TIMESTAMPADD(SQL_TSI_DAY, 14, CURRENT_TIMESTAMP)}, 'ACTIVE')
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to retrieve generated LoanID.");
    }

    private void updateMemberLoanCount(Connection conn, int memberId, int delta) throws SQLException {
        String sql = "UPDATE Members SET ActiveLoanCount = ActiveLoanCount + ? WHERE MemberID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, memberId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Member not found: " + memberId);
        }
    }

    private int[] getLoanDetails(Connection conn, int loanId) throws SQLException {
        String sql = "SELECT BookID, MemberID FROM Loans WHERE LoanID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new int[]{rs.getInt(1), rs.getInt(2)};
            }
        }
        return null;
    }

    private String getLoanStatus(Connection conn, int loanId) throws SQLException {
        String sql = "SELECT Status FROM Loans WHERE LoanID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
