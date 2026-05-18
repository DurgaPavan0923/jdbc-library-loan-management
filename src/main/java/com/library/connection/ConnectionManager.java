package com.library.connection;

import java.sql.*;

/**
 * Manages Derby embedded database connections and schema initialization.
 *
 * Responsibilities:
 *  - Configure embedded Derby JDBC URL
 *  - Create/verify schema (Members, Books, Loans tables + indexes)
 *  - Provide and close connections
 *  - Shutdown embedded Derby cleanly on exit
 */
public class ConnectionManager {

    private static final String DB_NAME       = "librarydb";
    private static final String JDBC_URL      = "jdbc:derby:" + DB_NAME + ";create=true";
    private static final String SHUTDOWN_URL  = "jdbc:derby:" + DB_NAME + ";shutdown=true";
    private static final String DRIVER_CLASS  = "org.apache.derby.jdbc.EmbeddedDriver";

    private static ConnectionManager instance;

    private ConnectionManager() {
        loadDriver();
    }

    /** Singleton accessor. */
    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Driver & Connection
    // -------------------------------------------------------------------------

    private void loadDriver() {
        try {
            Class.forName(DRIVER_CLASS);
            System.out.println("[ConnectionManager] Derby driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Apache Derby driver not found on classpath.", e);
        }
    }

    /**
     * Opens and returns a new JDBC connection with auto-commit ENABLED (default).
     * Callers that need transaction control must call conn.setAutoCommit(false) themselves.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    // -------------------------------------------------------------------------
    // Schema Initialization
    // -------------------------------------------------------------------------

    /**
     * Creates tables and indexes if they do not yet exist, then seeds baseline data.
     */
    public void initializeDatabase() {
        System.out.println("[ConnectionManager] Initializing database schema...");
        try (Connection conn = getConnection()) {
            createTables(conn);
            createIndexes(conn);
            seedData(conn);
            verifySchema(conn);
            System.out.println("[ConnectionManager] Database ready.\n");
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String[] ddl = {
            // Members table
            """
            CREATE TABLE Members (
                MemberID       INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                Name           VARCHAR(100) NOT NULL,
                Email          VARCHAR(150) NOT NULL UNIQUE,
                Phone          VARCHAR(20),
                MembershipDate DATE NOT NULL DEFAULT CURRENT_DATE,
                ActiveLoanCount INT NOT NULL DEFAULT 0,
                CONSTRAINT chk_active_loans CHECK (ActiveLoanCount >= 0)
            )
            """,

            // Books table
            """
            CREATE TABLE Books (
                BookID      INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                ISBN        VARCHAR(20)  NOT NULL UNIQUE,
                Title       VARCHAR(200) NOT NULL,
                Author      VARCHAR(100) NOT NULL,
                Genre       VARCHAR(50),
                PublishYear INT,
                Available   SMALLINT NOT NULL DEFAULT 1,
                CONSTRAINT chk_available CHECK (Available IN (0, 1))
            )
            """,

            // Loans table
            """
            CREATE TABLE Loans (
                LoanID     INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                BookID     INT NOT NULL,
                MemberID   INT NOT NULL,
                LoanDate   DATE NOT NULL DEFAULT CURRENT_DATE,
                DueDate    DATE NOT NULL,
                ReturnDate DATE,
                Status     VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
                CONSTRAINT fk_loan_book   FOREIGN KEY (BookID)   REFERENCES Books(BookID),
                CONSTRAINT fk_loan_member FOREIGN KEY (MemberID) REFERENCES Members(MemberID),
                CONSTRAINT chk_status     CHECK (Status IN ('ACTIVE','RETURNED','OVERDUE'))
            )
            """
        };

        DatabaseMetaData meta = conn.getMetaData();
        String[] tableNames = {"MEMBERS", "BOOKS", "LOANS"};

        for (int i = 0; i < ddl.length; i++) {
            String tableName = tableNames[i];
            try (ResultSet rs = meta.getTables(null, "APP", tableName, new String[]{"TABLE"})) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(ddl[i]);
                        System.out.println("[ConnectionManager] Created table: " + tableName);
                    }
                } else {
                    System.out.println("[ConnectionManager] Table already exists: " + tableName);
                }
            }
        }
    }

    private void createIndexes(Connection conn) throws SQLException {
        String[][] indexes = {
            {"idx_books_isbn",         "CREATE INDEX idx_books_isbn         ON Books(ISBN)"},
            {"idx_loans_memberid",     "CREATE INDEX idx_loans_memberid     ON Loans(MemberID)"},
            {"idx_loans_returndate",   "CREATE INDEX idx_loans_returndate   ON Loans(ReturnDate)"},
            {"idx_loans_status",       "CREATE INDEX idx_loans_status       ON Loans(Status)"},
            {"idx_loans_duedate",      "CREATE INDEX idx_loans_duedate      ON Loans(DueDate)"}
        };

        DatabaseMetaData meta = conn.getMetaData();
        for (String[] idx : indexes) {
            String idxName = idx[0].toUpperCase();
            try (ResultSet rs = meta.getIndexInfo(null, "APP", "LOANS", false, false)) {
                boolean exists = false;
                while (rs.next()) {
                    String name = rs.getString("INDEX_NAME");
                    if (idxName.equals(name)) { exists = true; break; }
                }
                if (!exists) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(idx[1]);
                        System.out.println("[ConnectionManager] Created index: " + idx[0]);
                    } catch (SQLException e) {
                        if (!"X0Y32".equals(e.getSQLState())) throw e; // ignore "already exists"
                    }
                }
            }
        }
    }

    private void seedData(Connection conn) throws SQLException {
        // Check if data already seeded
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Members")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                System.out.println("[ConnectionManager] Seed data already present, skipping.");
                return;
            }
        }

        System.out.println("[ConnectionManager] Seeding baseline data...");
        conn.setAutoCommit(false);
        try {
            // Seed Members
            String insertMember = "INSERT INTO Members (Name, Email, Phone) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertMember)) {
                Object[][] members = {
                    {"Alice Johnson", "alice@library.com", "555-0101"},
                    {"Bob Smith",     "bob@library.com",   "555-0102"},
                    {"Carol White",   "carol@library.com", "555-0103"},
                    {"David Brown",   "david@library.com", "555-0104"},
                    {"Eva Martinez",  "eva@library.com",   "555-0105"}
                };
                for (Object[] m : members) {
                    ps.setString(1, (String) m[0]);
                    ps.setString(2, (String) m[1]);
                    ps.setString(3, (String) m[2]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Seed Books
            String insertBook = "INSERT INTO Books (ISBN, Title, Author, Genre, PublishYear) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertBook)) {
                Object[][] books = {
                    {"978-0-13-468599-1", "Effective Java",                   "Joshua Bloch",       "Technology",  2018},
                    {"978-0-13-235088-4", "Clean Code",                       "Robert C. Martin",   "Technology",  2008},
                    {"978-0-13-110362-7", "The C Programming Language",       "Kernighan & Ritchie","Technology",  1988},
                    {"978-0-06-112008-4", "To Kill a Mockingbird",            "Harper Lee",         "Fiction",     1960},
                    {"978-0-7432-7356-5", "The Great Gatsby",                 "F. Scott Fitzgerald","Fiction",     1925},
                    {"978-0-14-028329-7", "1984",                             "George Orwell",      "Fiction",     1949},
                    {"978-0-525-55360-5", "The Midnight Library",             "Matt Haig",          "Fiction",     2020},
                    {"978-0-385-54734-9", "Educated",                         "Tara Westover",      "Memoir",      2018},
                    {"978-1-250-30165-3", "Atomic Habits",                    "James Clear",        "Self-Help",   2018},
                    {"978-0-14-044913-6", "The Art of War",                   "Sun Tzu",            "Philosophy",  500}
                };
                for (Object[] b : books) {
                    ps.setString(1, (String) b[0]);
                    ps.setString(2, (String) b[1]);
                    ps.setString(3, (String) b[2]);
                    ps.setString(4, (String) b[3]);
                    ps.setInt(5, (int) b[4]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            System.out.println("[ConnectionManager] Seed data inserted (5 members, 10 books).");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void verifySchema(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        System.out.println("[ConnectionManager] Schema verification:");
        for (String table : new String[]{"MEMBERS", "BOOKS", "LOANS"}) {
            try (ResultSet rs = meta.getTables(null, "APP", table, new String[]{"TABLE"})) {
                System.out.printf("  %-10s : %s%n", table, rs.next() ? "OK" : "MISSING!");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    /**
     * Cleanly shuts down the embedded Derby instance, releasing file locks.
     * Must be called once before JVM exit.
     */
    public void shutdown() {
        System.out.println("\n[ConnectionManager] Shutting down Derby...");
        try {
            DriverManager.getConnection(SHUTDOWN_URL);
        } catch (SQLException e) {
            // Derby always throws SQLState 08006 on successful per-database shutdown
            if ("08006".equals(e.getSQLState()) || "XJ015".equals(e.getSQLState())) {
                System.out.println("[ConnectionManager] Derby shut down cleanly.");
            } else {
                System.err.println("[ConnectionManager] Unexpected shutdown error: " + e.getMessage());
            }
        }
    }
}
