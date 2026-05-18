package com.library.model;

import java.time.LocalDate;

/**
 * Represents a book loan record.
 */
public class Loan {
    private int loanId;
    private int bookId;
    private int memberId;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status; // ACTIVE, RETURNED, OVERDUE

    // Joined fields for display
    private String bookTitle;
    private String memberName;

    public Loan() {}

    public Loan(int bookId, int memberId) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.loanDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(14); // 2-week loan period
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public int getLoanId()                  { return loanId; }
    public void setLoanId(int loanId)       { this.loanId = loanId; }

    public int getBookId()                  { return bookId; }
    public void setBookId(int bookId)       { this.bookId = bookId; }

    public int getMemberId()                { return memberId; }
    public void setMemberId(int memberId)   { this.memberId = memberId; }

    public LocalDate getLoanDate()                  { return loanDate; }
    public void setLoanDate(LocalDate loanDate)     { this.loanDate = loanDate; }

    public LocalDate getDueDate()                   { return dueDate; }
    public void setDueDate(LocalDate dueDate)       { this.dueDate = dueDate; }

    public LocalDate getReturnDate()                    { return returnDate; }
    public void setReturnDate(LocalDate returnDate)     { this.returnDate = returnDate; }

    public String getStatus()               { return status; }
    public void setStatus(String status)    { this.status = status; }

    public String getBookTitle()                    { return bookTitle; }
    public void setBookTitle(String bookTitle)      { this.bookTitle = bookTitle; }

    public String getMemberName()                   { return memberName; }
    public void setMemberName(String memberName)    { this.memberName = memberName; }

    public boolean isOverdue() {
        return "ACTIVE".equals(status) && LocalDate.now().isAfter(dueDate);
    }

    @Override
    public String toString() {
        return String.format("Loan{id=%d, book='%s', member='%s', due=%s, status='%s'}",
                loanId, bookTitle, memberName, dueDate, status);
    }
}
