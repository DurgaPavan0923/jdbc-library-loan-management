# Library Loan Management System

![Java](https://img.shields.io/badge/Java-17-orange)
![Maven](https://img.shields.io/badge/Maven-3.8+-blue)
![Database](https://img.shields.io/badge/Database-Apache_Derby-green)
![License](https://img.shields.io/badge/License-Educational-lightgrey)

A console-driven JDBC application demonstrating **explicit transaction management**, **ACID compliance**, and **performance benchmarking** using Apache Derby (embedded mode).

---

# Table of Contents

1. Project Overview
2. Architecture
3. Prerequisites
4. Build & Run
5. Project Structure
6. Feature Walkthrough
7. Transaction Management
8. Performance Benchmarks
9. Database Schema
10. Sample CLI Session
11. Screenshots
12. Learning Outcomes
13. Future Enhancements
14. Dependency List
15. Author
16. License

---

# Project Overview

| Item | Detail |
|------|--------|
| Language | Java 17 |
| Database | Apache Derby 10.16 (embedded) |
| Build Tool | Maven 3.8+ |
| Pattern | Layered Architecture |
| Type | Console-based JDBC Application |
| Features | Transactions, Savepoints, Benchmarking |

---

# Architecture

```text
UI Layer
   ↓
Business Logic Layer
   ↓
Transaction Management Layer
   ↓
Connection Layer
   ↓
Apache Derby Database
```

---

# Package Structure

```text
com.library
├── connection/
│   └── ConnectionManager.java
├── transaction/
│   └── TransactionService.java
├── business/
│   └── BusinessLogic.java
├── benchmark/
│   └── PerformanceEvaluator.java
├── model/
│   ├── Member.java
│   ├── Book.java
│   └── Loan.java
└── ui/
    └── MainApp.java
```

---

# Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 17+ |
| Maven | 3.8+ |
| Git | Latest |
| Internet | Required for first Maven build |

---

# Build & Run

## Step 1 — Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/library-loan-system.git
cd library-loan-system
```

## Step 2 — Build Project

```bash
mvn clean package
```

## Step 3 — Run Application

```bash
java -jar target/library-loan-system.jar
```

---

# Project Structure

```text
library-loan-system/
├── pom.xml
├── README.md
├── .gitignore
│
├── docs/
│   └── analysis.md
│
├── screenshots/
│   ├── main-menu.png
│   ├── loan-processing.png
│   └── benchmark.png
│
├── src/main/java/com/library/
│   ├── connection/
│   ├── transaction/
│   ├── business/
│   ├── benchmark/
│   ├── model/
│   └── ui/
│
├── librarydb/
│
└── performance_report.csv
```

---

# Feature Walkthrough

## 1. Member Management
- Add new members
- View all members
- Search member details

## 2. Book Catalog
- Add books
- View availability status
- Search books

## 3. Loan Operations
- Process book loans
- Process returns
- Automatic availability updates

## 4. Transaction Management
- Explicit commit and rollback
- Savepoint creation
- Partial transaction recovery

## 5. Reports & Queries
- Active loans
- Member loan history
- Book availability reports

## 6. Performance Evaluation
- Insert benchmark
- Batch processing benchmark
- Query performance comparison

---

# Transaction Management

## processLoan() Workflow

```text
setAutoCommit(false)
        │
        ├── Check Book Availability
        │
        ├── Savepoint SP1
        ├── Update Book Status
        │
        ├── Savepoint SP2
        ├── Insert Loan Record
        │
        ├── Update Member Loan Count
        │
        ├── SUCCESS → COMMIT
        │
        └── FAILURE → ROLLBACK
```

---

# ACID Properties Demonstrated

| Property | Implementation |
|----------|---------------|
| Atomicity | Commit / Rollback |
| Consistency | Constraints & Validation |
| Isolation | Transaction Isolation Levels |
| Durability | Derby Persistent Storage |

---

# Performance Benchmarks

## Benchmark Suites

| Test | Description |
|------|-------------|
| Insert Benchmark | Individual vs Batch Insert |
| Query Benchmark | Indexed vs Non-indexed Queries |
| Statement Benchmark | Statement vs PreparedStatement |
| Transaction Benchmark | AutoCommit vs Manual Commit |

---

# Benchmark Methodology

- Warm-up runs discarded
- Average execution time measured
- Uses `System.nanoTime()`
- CSV report generated automatically

---

# Database Schema

## Members Table

```text
member_id (PK)
name
email
phone
```

## Books Table

```text
book_id (PK)
title
author
genre
available
```

## Loans Table

```text
loan_id (PK)
member_id (FK)
book_id (FK)
loan_date
return_date
```

---

# Sample CLI Session

```text
╔══════════════════════════════════════╗
║   Library Loan Management System     ║
╠══════════════════════════════════════╣
║  1. Add Member                       ║
║  2. Add Book                         ║
║  3. Loan Book                        ║
║  4. Return Book                      ║
║  5. View Reports                     ║
║  6. Run Benchmarks                   ║
║  0. Exit                             ║
╚══════════════════════════════════════╝
```

---

# Screenshots

## Main Menu
![Main Menu](screenshots/main-menu.png)

## Loan Processing
![Loan Processing](screenshots/loan-processing.png)

## Benchmark Results
![Benchmark](screenshots/benchmark.png)

---

# Learning Outcomes

This project demonstrates practical understanding of:

- JDBC API
- Apache Derby Database
- ACID Transactions
- Savepoints & Rollback
- Performance Benchmarking
- Layered Software Architecture
- Exception Handling
- PreparedStatement Usage
- Resource Management

---

# Future Enhancements

- JavaFX GUI
- Spring Boot REST API
- Authentication System
- Cloud Database Integration
- Multi-user Concurrency Simulation
- PDF/Excel Report Export

---

# Dependency List

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Apache Derby | 10.16.1.1 | Embedded Database |
| Maven | 3.8+ | Build Tool |
| Java 17 | Latest | Application Runtime |

---

# Author

**Rajana Durga Pavan Kumar**  
B.Tech CSE (AI & ML)  
Institute of Technical Education and Research (ITER), SOA University

GitHub: https://github.com/DurgaPavan0923

---

# License

This project is developed for academic and educational purposes.
