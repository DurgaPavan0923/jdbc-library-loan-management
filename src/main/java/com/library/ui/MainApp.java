package com.library.ui;

import com.library.benchmark.PerformanceEvaluator;
import com.library.business.BusinessLogic;
import com.library.connection.ConnectionManager;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.transaction.TransactionService;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Console-driven entry point for the Library Loan Management System.
 *
 * Orchestrates:
 *  - Database initialization (ConnectionManager)
 *  - Business operations (BusinessLogic + TransactionService)
 *  - Performance benchmarks (PerformanceEvaluator)
 *  - Graceful shutdown
 */
public class MainApp {

    private static ConnectionManager  cm;
    private static BusinessLogic      bl;
    private static TransactionService ts;
    private static PerformanceEvaluator pe;
    private static Scanner scanner;

    // =========================================================================
    // main
    // =========================================================================

    public static void main(String[] args) {
        printBanner();

        // Bootstrap
        cm      = ConnectionManager.getInstance();
        cm.initializeDatabase();

        bl      = new BusinessLogic(cm);
        ts      = new TransactionService(cm);
        pe      = new PerformanceEvaluator(cm);
        scanner = new Scanner(System.in);

        // Register shutdown hook so Derby closes even on Ctrl-C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cm.shutdown();
            System.out.println("[MainApp] Goodbye!");
        }));

        runMainMenu();
    }

    // =========================================================================
    // Menus
    // =========================================================================

    private static void runMainMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║   Library Loan Management System     ║");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  1. Member Management                ║");
            System.out.println("║  2. Book Catalog                     ║");
            System.out.println("║  3. Loan Operations                  ║");
            System.out.println("║  4. Query & Reports                  ║");
            System.out.println("║  5. Transaction Demonstrations       ║");
            System.out.println("║  6. Performance Benchmarks           ║");
            System.out.println("║  0. Exit                             ║");
            System.out.println("╚══════════════════════════════════════╝");
            System.out.print("  Select: ");

            int choice = readInt();
            switch (choice) {
                case 1 -> memberMenu();
                case 2 -> bookMenu();
                case 3 -> loanMenu();
                case 4 -> queryMenu();
                case 5 -> transactionDemoMenu();
                case 6 -> {
                    System.out.println("\n[MainApp] Starting benchmarks (this may take a minute)...");
                    pe.runAll();
                }
                case 0 -> running = false;
                default -> System.out.println("  Invalid choice.");
            }
        }
    }

    // ── Member Menu ──────────────────────────────────────────────────────────

    private static void memberMenu() {
        System.out.println("\n── Member Management ───────────────────");
        System.out.println("  1. Register new member");
        System.out.println("  2. List all members");
        System.out.println("  3. View member details");
        System.out.println("  0. Back");
        System.out.print("  Select: ");

        switch (readInt()) {
            case 1 -> registerMember();
            case 2 -> listMembers();
            case 3 -> viewMember();
            default -> {}
        }
    }

    private static void registerMember() {
        System.out.println("\n── Register New Member ─");
        String name  = prompt("  Name  : ");
        String email = prompt("  Email : ");
        String phone = prompt("  Phone : ");
        try {
            int id = bl.registerMember(name, email, phone);
            System.out.println("  ✓ Member registered with ID: " + id);
        } catch (SQLException e) {
            System.err.println("  ✗ Error: " + e.getMessage());
        }
    }

    private static void listMembers() {
        try {
            List<Member> members = bl.getAllMembers();
            System.out.printf("%n  %-4s  %-22s  %-28s  %-12s  %s%n",
                    "ID", "Name", "Email", "Phone", "ActiveLoans");
            System.out.println("  " + "-".repeat(82));
            for (Member m : members) {
                System.out.printf("  %-4d  %-22s  %-28s  %-12s  %d%n",
                        m.getMemberId(), m.getName(), m.getEmail(),
                        m.getPhone(), m.getActiveLoanCount());
            }
        } catch (SQLException e) {
            System.err.println("  ✗ Error: " + e.getMessage());
        }
    }

    private static void viewMember() {
        int id = promptInt("  Member ID: ");
        try {
            Member m = bl.getMemberById(id);
            if (m == null) { System.out.println("  Not found."); return; }
            System.out.printf("%n  ID           : %d%n", m.getMemberId());
            System.out.printf("  Name         : %s%n", m.getName());
            System.out.printf("  Email        : %s%n", m.getEmail());
            System.out.printf("  Phone        : %s%n", m.getPhone());
            System.out.printf("  Joined       : %s%n", m.getMembershipDate());
            System.out.printf("  Active Loans : %d%n", m.getActiveLoanCount());
        } catch (SQLException e) {
            System.err.println("  ✗ Error: " + e.getMessage());
        }
    }

    // ── Book Menu ────────────────────────────────────────────────────────────

    private static void bookMenu() {
        System.out.println("\n── Book Catalog ───────────────────────");
        System.out.println("  1. Add new book");
        System.out.println("  2. List all books");
        System.out.println("  3. View book details");
        System.out.println("  0. Back");
        System.out.print("  Select: ");

        switch (readInt()) {
            case 1 -> addBook();
            case 2 -> listBooks();
            case 3 -> viewBook();
            default -> {}
        }
    }

    private static void addBook() {
        System.out.println("\n── Add New Book ─");
        String isbn   = prompt("  ISBN        : ");
        String title  = prompt("  Title       : ");
        String author = prompt("  Author      : ");
        String genre  = prompt("  Genre       : ");
        int    year   = promptInt("  Publish Year: ");
        try {
            int id = bl.addBook(isbn, title, author, genre, year);
            System.out.println("  ✓ Book added with ID: " + id);
        } catch (SQLException e) {
            System.err.println("  ✗ Error: " + e.getMessage());
        }
    }

    private static void listBooks() {
        try {
            List<Book> books = bl.getAllBooks();
            System.out.printf("%n  %-4s  %-20s  %-30s  %-20s  %-12s  %s%n",
                    "ID", "ISBN", "Title", "Author", "Genre", "Available");
            System.out.println("  " + "-".repeat(100));
            for (Book b : books) {
                System.out.printf("  %-4d  %-20s  %-30s  %-20s  %-12s  %s%n",
                        b.getBookId(), b.getIsbn(), truncate(b.getTitle(), 29),
                        truncate(b.getAuthor(), 19), b.getGenre(),
                        b.isAvailable() ? "✓ Yes" : "✗ No");
            }
        } catch (SQLException e) {
            System.err.println("  ✗ Error: " + e.getMessage());
        }
    }

    private static void viewBook() {
        int id = promptInt("  Book ID: ");
        try {
            Book b = bl.getBookById(id);
            if (b == null) { System.out.println("  Not found."); return; }
            System.out.printf("%n  ID          : %d%n", b.getBookId());
            System.out.printf("  ISBN        : %s%n", b.getIsbn());
            System.out.printf("  Title       : %s%n", b.getTitle());
            System.out.printf("  Author      : %s%n", b.getAuthor());
            System.out.printf("  Genre       : %s%n", b.getGenre());
            System.out.printf("  Year        : %d%n", b.getPublishYear());
            System.out.printf("  Available   : %s%n", b.isAvailable() ? "Yes" : "No");
        } catch (SQLException e) {
            System.err.println("  ✗ Error: " + e.getMessage());
        }
    }

    // ── Loan Menu ────────────────────────────────────────────────────────────

    private static void loanMenu() {
        System.out.println("\n── Loan Operations ────────────────────");
        System.out.println("  1. Process new loan");
        System.out.println("  2. Process return");
        System.out.println("  0. Back");
        System.out.print("  Select: ");

        switch (readInt()) {
            case 1 -> processLoan();
            case 2 -> processReturn();
            default -> {}
        }
    }

    private static void processLoan() {
        System.out.println("\n── Process Loan ─");
        int bookId   = promptInt("  Book ID  : ");
        int memberId = promptInt("  Member ID: ");
        try {
            int loanId = ts.processLoan(bookId, memberId);
            System.out.println("  ✓ Loan processed. Loan ID: " + loanId);
        } catch (SQLException e) {
            System.err.println("  ✗ Loan failed: " + e.getMessage());
        }
    }

    private static void processReturn() {
        System.out.println("\n── Process Return ─");
        int loanId = promptInt("  Loan ID: ");
        try {
            ts.processReturn(loanId);
            System.out.println("  ✓ Return processed.");
        } catch (SQLException e) {
            System.err.println("  ✗ Return failed: " + e.getMessage());
        }
    }

    // ── Query Menu ───────────────────────────────────────────────────────────

    private static void queryMenu() {
        System.out.println("\n── Query & Reports ────────────────────");
        System.out.println("  1. Active loans");
        System.out.println("  2. Loans by member");
        System.out.println("  3. Overdue loans");
        System.out.println("  0. Back");
        System.out.print("  Select: ");

        switch (readInt()) {
            case 1 -> showActiveLoans();
            case 2 -> showLoansByMember();
            case 3 -> showOverdueLoans();
            default -> {}
        }
    }

    private static void showActiveLoans() {
        try {
            List<Loan> loans = bl.getActiveLoans();
            printLoans(loans, "Active Loans");
        } catch (SQLException e) {
            System.err.println("  ✗ Error: " + e.getMessage());
        }
    }

    private static void showLoansByMember() {
        int memberId = promptInt("  Member ID: ");
        try {
            List<Loan> loans = bl.getLoansByMember(memberId);
            printLoans(loans, "Loans for Member " + memberId);
        } catch (SQLException e) {
            System.err.println("  ✗ Error: " + e.getMessage());
        }
    }

    private static void showOverdueLoans() {
        try {
            List<Loan> loans = bl.getOverdueLoans();
            printLoans(loans, "Overdue Loans");
        } catch (SQLException e) {
            System.err.println("  ✗ Error: " + e.getMessage());
        }
    }

    private static void printLoans(List<Loan> loans, String title) {
        System.out.printf("%n  ── %s (%d) ──%n", title, loans.size());
        if (loans.isEmpty()) { System.out.println("  (none)"); return; }
        System.out.printf("  %-6s  %-28s  %-20s  %-12s  %-12s  %s%n",
                "LoanID", "Book", "Member", "LoanDate", "DueDate", "Status");
        System.out.println("  " + "-".repeat(100));
        for (Loan l : loans) {
            System.out.printf("  %-6d  %-28s  %-20s  %-12s  %-12s  %s%n",
                    l.getLoanId(), truncate(l.getBookTitle(), 27),
                    truncate(l.getMemberName(), 19),
                    l.getLoanDate(), l.getDueDate(), l.getStatus());
        }
    }

    // ── Transaction Demo Menu ────────────────────────────────────────────────

    private static void transactionDemoMenu() {
        System.out.println("\n── Transaction Demonstrations ─────────");
        System.out.println("  1. Demonstrate savepoint rollback (invalid memberId)");
        System.out.println("  2. Demonstrate constraint violation + rollback");
        System.out.println("  3. Demonstrate isolation level (READ_COMMITTED)");
        System.out.println("  0. Back");
        System.out.print("  Select: ");

        switch (readInt()) {
            case 1 -> {
                System.out.println("\n[Demo] Attempting loan with invalid memberId=9999...");
                try {
                    ts.processLoan(1, 9999);
                } catch (SQLException e) {
                    System.out.println("[Demo] Expected failure handled. Rollback verified.");
                }
            }
            case 2 -> ts.demonstrateConstraintViolation("978-0-13-468599-1"); // duplicate ISBN
            case 3 -> {
                try {
                    ts.demonstrateIsolation(1);
                } catch (SQLException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
            default -> {}
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static int readInt() {
        try {
            String line = scanner.nextLine().trim();
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int promptInt(String msg) {
        System.out.print(msg);
        return readInt();
    }

    private static String prompt(String msg) {
        System.out.print(msg);
        return scanner.nextLine().trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static void printBanner() {
        System.out.println("""
                ╔══════════════════════════════════════════════════════════╗
                ║     Library Loan Management System                       ║
                ║     JDBC + Apache Derby | Transaction Management Demo    ║
                ╚══════════════════════════════════════════════════════════╝
                """);
    }
}
