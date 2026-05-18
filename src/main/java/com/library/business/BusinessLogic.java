package com.library.business;

import com.library.connection.ConnectionManager;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Core CRUD and query operations for the Library Loan Management System.
 *
 * All mutations use PreparedStatement to prevent SQL injection.
 * All resources are managed with try-with-resources.
 */
public class BusinessLogic {

    private final ConnectionManager connectionManager;

    public BusinessLogic(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // =========================================================================
    // Member Operations
    // =========================================================================

    /** Registers a new member and returns the generated MemberID. */
    public int registerMember(String name, String email, String phone) throws SQLException {
        String sql = "INSERT INTO Members (Name, Email, Phone) VALUES (?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    System.out.println("[BusinessLogic] Member registered: " + name + " (ID=" + id + ")");
                    return id;
                }
            }
        }
        throw new SQLException("Failed to obtain generated MemberID.");
    }

    /** Returns all members. */
    public List<Member> getAllMembers() throws SQLException {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT MemberID, Name, Email, Phone, MembershipDate, ActiveLoanCount FROM Members ORDER BY MemberID";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Member m = new Member();
                m.setMemberId(rs.getInt("MemberID"));
                m.setName(rs.getString("Name"));
                m.setEmail(rs.getString("Email"));
                m.setPhone(rs.getString("Phone"));
                if (rs.getDate("MembershipDate") != null)
                    m.setMembershipDate(rs.getDate("MembershipDate").toLocalDate());
                m.setActiveLoanCount(rs.getInt("ActiveLoanCount"));
                list.add(m);
            }
        }
        return list;
    }

    /** Finds a member by ID; returns null if not found. */
    public Member getMemberById(int memberId) throws SQLException {
        String sql = "SELECT MemberID, Name, Email, Phone, MembershipDate, ActiveLoanCount "
                   + "FROM Members WHERE MemberID = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Member m = new Member();
                    m.setMemberId(rs.getInt("MemberID"));
                    m.setName(rs.getString("Name"));
                    m.setEmail(rs.getString("Email"));
                    m.setPhone(rs.getString("Phone"));
                    if (rs.getDate("MembershipDate") != null)
                        m.setMembershipDate(rs.getDate("MembershipDate").toLocalDate());
                    m.setActiveLoanCount(rs.getInt("ActiveLoanCount"));
                    return m;
                }
            }
        }
        return null;
    }

    // =========================================================================
    // Book Operations
    // =========================================================================

    /** Adds a new book to the catalog; returns generated BookID. */
    public int addBook(String isbn, String title, String author, String genre, int publishYear)
            throws SQLException {
        String sql = "INSERT INTO Books (ISBN, Title, Author, Genre, PublishYear) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, isbn);
            ps.setString(2, title);
            ps.setString(3, author);
            ps.setString(4, genre);
            ps.setInt(5, publishYear);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    System.out.println("[BusinessLogic] Book added: \"" + title + "\" (ID=" + id + ")");
                    return id;
                }
            }
        }
        throw new SQLException("Failed to obtain generated BookID.");
    }

    /** Returns all books in the catalog. */
    public List<Book> getAllBooks() throws SQLException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT BookID, ISBN, Title, Author, Genre, PublishYear, Available FROM Books ORDER BY BookID";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Book b = new Book();
                b.setBookId(rs.getInt("BookID"));
                b.setIsbn(rs.getString("ISBN"));
                b.setTitle(rs.getString("Title"));
                b.setAuthor(rs.getString("Author"));
                b.setGenre(rs.getString("Genre"));
                b.setPublishYear(rs.getInt("PublishYear"));
                b.setAvailable(rs.getInt("Available") == 1);
                list.add(b);
            }
        }
        return list;
    }

    /** Finds a book by ID; returns null if not found. */
    public Book getBookById(int bookId) throws SQLException {
        String sql = "SELECT BookID, ISBN, Title, Author, Genre, PublishYear, Available FROM Books WHERE BookID = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book b = new Book();
                    b.setBookId(rs.getInt("BookID"));
                    b.setIsbn(rs.getString("ISBN"));
                    b.setTitle(rs.getString("Title"));
                    b.setAuthor(rs.getString("Author"));
                    b.setGenre(rs.getString("Genre"));
                    b.setPublishYear(rs.getInt("PublishYear"));
                    b.setAvailable(rs.getInt("Available") == 1);
                    return b;
                }
            }
        }
        return null;
    }

    // =========================================================================
    // Loan Queries
    // =========================================================================

    /** Returns all active loans with book and member names (JOIN query). */
    public List<Loan> getActiveLoans() throws SQLException {
        List<Loan> list = new ArrayList<>();
        String sql = """
            SELECT l.LoanID, l.BookID, l.MemberID, l.LoanDate, l.DueDate, l.ReturnDate, l.Status,
                   b.Title AS BookTitle, m.Name AS MemberName
            FROM   Loans l
            JOIN   Books   b ON l.BookID   = b.BookID
            JOIN   Members m ON l.MemberID = m.MemberID
            WHERE  l.Status = 'ACTIVE'
            ORDER BY l.DueDate
            """;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            list = mapLoans(rs);
        }
        return list;
    }

    /** Returns all active loans for a specific member. */
    public List<Loan> getLoansByMember(int memberId) throws SQLException {
        String sql = """
            SELECT l.LoanID, l.BookID, l.MemberID, l.LoanDate, l.DueDate, l.ReturnDate, l.Status,
                   b.Title AS BookTitle, m.Name AS MemberName
            FROM   Loans l
            JOIN   Books   b ON l.BookID   = b.BookID
            JOIN   Members m ON l.MemberID = m.MemberID
            WHERE  l.MemberID = ?
            ORDER BY l.LoanDate DESC
            """;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapLoans(rs);
            }
        }
    }

    /** Returns all loans where DueDate has passed and book not returned (overdue). */
    public List<Loan> getOverdueLoans() throws SQLException {
        String sql = """
            SELECT l.LoanID, l.BookID, l.MemberID, l.LoanDate, l.DueDate, l.ReturnDate, l.Status,
                   b.Title AS BookTitle, m.Name AS MemberName
            FROM   Loans l
            JOIN   Books   b ON l.BookID   = b.BookID
            JOIN   Members m ON l.MemberID = m.MemberID
            WHERE  l.Status = 'ACTIVE'
              AND  l.DueDate < CURRENT_DATE
            ORDER BY l.DueDate
            """;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapLoans(rs);
        }
    }

    /** Returns a single loan by ID. */
    public Loan getLoanById(int loanId) throws SQLException {
        String sql = """
            SELECT l.LoanID, l.BookID, l.MemberID, l.LoanDate, l.DueDate, l.ReturnDate, l.Status,
                   b.Title AS BookTitle, m.Name AS MemberName
            FROM   Loans l
            JOIN   Books   b ON l.BookID   = b.BookID
            JOIN   Members m ON l.MemberID = m.MemberID
            WHERE  l.LoanID = ?
            """;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Loan> loans = mapLoans(rs);
                return loans.isEmpty() ? null : loans.get(0);
            }
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private List<Loan> mapLoans(ResultSet rs) throws SQLException {
        List<Loan> list = new ArrayList<>();
        while (rs.next()) {
            Loan l = new Loan();
            l.setLoanId(rs.getInt("LoanID"));
            l.setBookId(rs.getInt("BookID"));
            l.setMemberId(rs.getInt("MemberID"));
            if (rs.getDate("LoanDate") != null)
                l.setLoanDate(rs.getDate("LoanDate").toLocalDate());
            if (rs.getDate("DueDate") != null)
                l.setDueDate(rs.getDate("DueDate").toLocalDate());
            if (rs.getDate("ReturnDate") != null)
                l.setReturnDate(rs.getDate("ReturnDate").toLocalDate());
            l.setStatus(rs.getString("Status"));
            l.setBookTitle(rs.getString("BookTitle"));
            l.setMemberName(rs.getString("MemberName"));
            list.add(l);
        }
        return list;
    }
}
