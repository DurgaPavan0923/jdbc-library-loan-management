package com.library.model;

/**
 * Represents a book in the library catalog.
 */
public class Book {
    private int bookId;
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private int publishYear;
    private boolean available;

    public Book() {}

    public Book(String isbn, String title, String author, String genre, int publishYear) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publishYear = publishYear;
        this.available = true;
    }

    // Getters and Setters
    public int getBookId()                  { return bookId; }
    public void setBookId(int bookId)       { this.bookId = bookId; }

    public String getIsbn()                 { return isbn; }
    public void setIsbn(String isbn)        { this.isbn = isbn; }

    public String getTitle()                { return title; }
    public void setTitle(String title)      { this.title = title; }

    public String getAuthor()               { return author; }
    public void setAuthor(String author)    { this.author = author; }

    public String getGenre()                { return genre; }
    public void setGenre(String genre)      { this.genre = genre; }

    public int getPublishYear()                     { return publishYear; }
    public void setPublishYear(int publishYear)     { this.publishYear = publishYear; }

    public boolean isAvailable()                    { return available; }
    public void setAvailable(boolean available)     { this.available = available; }

    @Override
    public String toString() {
        return String.format("Book{id=%d, isbn='%s', title='%s', author='%s', available=%s}",
                bookId, isbn, title, author, available);
    }
}
