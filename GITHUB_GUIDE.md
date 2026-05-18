# Step-by-Step GitHub Guide
## Library Loan Management System — JDBC + Apache Derby

---

## PHASE 0 — Prerequisites

Before uploading to GitHub, confirm the following tools are installed.

### 0.1 Verify Java 17+
```bash
java -version
# Expected: openjdk 17.x.x or higher
```
Install if missing: https://adoptium.net

### 0.2 Verify Maven
```bash
mvn -version
# Expected: Apache Maven 3.8.x or higher
```
Install if missing: https://maven.apache.org/download.cgi

### 0.3 Verify Git
```bash
git --version
# Expected: git version 2.x.x
```
Install if missing: https://git-scm.com/downloads

### 0.4 Create a GitHub Account
If you don't have one: https://github.com/signup

---

## PHASE 1 — Set Up the Project Locally

### 1.1 Create the Root Directory
```bash
mkdir library-loan-system
cd library-loan-system
```

### 1.2 Create the Maven Directory Structure
```bash
mkdir -p src/main/java/com/library/connection
mkdir -p src/main/java/com/library/transaction
mkdir -p src/main/java/com/library/business
mkdir -p src/main/java/com/library/benchmark
mkdir -p src/main/java/com/library/model
mkdir -p src/main/java/com/library/ui
mkdir -p docs
```

### 1.3 Place All Source Files

Copy each file to its correct location:

```
library-loan-system/
│
├── pom.xml
├── README.md
│
├── docs/
│   └── analysis.md
│
└── src/main/java/com/library/
    ├── model/
    │   ├── Member.java
    │   ├── Book.java
    │   └── Loan.java
    ├── connection/
    │   └── ConnectionManager.java
    ├── transaction/
    │   └── TransactionService.java
    ├── business/
    │   └── BusinessLogic.java
    ├── benchmark/
    │   └── PerformanceEvaluator.java
    └── ui/
        └── MainApp.java
```

### 1.4 Verify the Build Compiles
```bash
mvn clean compile
```
Expected output ends with `BUILD SUCCESS`.

### 1.5 Run a Smoke Test
```bash
mvn clean package -q
java -jar target/library-loan-system.jar
```
The banner should print and the main menu should appear.
Type `0` and press Enter to exit.

### 1.6 Clean Up Derby Database Files
The run created `librarydb/`. Add it to `.gitignore` (next step).

---

## PHASE 2 — Prepare for Git

### 2.1 Create `.gitignore`
```bash
cat > .gitignore << 'EOF'
# Maven build output
target/

# Derby embedded database files (auto-created on first run)
librarydb/

# Performance report (generated at runtime)
performance_report.csv

# IDE files
.idea/
*.iml
.vscode/
*.class

# macOS
.DS_Store

# Windows
Thumbs.db
EOF
```

### 2.2 Initialize a Local Git Repository
```bash
git init
```

### 2.3 Stage All Files
```bash
git add .
```

### 2.4 Verify What Will Be Committed
```bash
git status
```
You should see all `.java` files, `pom.xml`, `README.md`, `docs/analysis.md`, and `.gitignore` listed as new files.
You should NOT see `target/`, `librarydb/`, or `performance_report.csv`.

### 2.5 Make the First Commit
```bash
git commit -m "Initial commit: Library Loan Management System

- ConnectionManager: Derby embedded DB, schema DDL, seed data, shutdown hook
- TransactionService: explicit commit/rollback/savepoints, isolation demo
- BusinessLogic: CRUD operations, JOIN queries, PreparedStatement throughout
- PerformanceEvaluator: 4 benchmark suites with stats and CSV report
- MainApp: CLI menu, workflow orchestration
- Model classes: Member, Book, Loan
- pom.xml: Maven build with Derby 10.16 dependency
- README.md and docs/analysis.md"
```

---

## PHASE 3 — Create the GitHub Repository

### 3.1 Log In to GitHub
Open https://github.com and sign in.

### 3.2 Create a New Repository
1. Click the **+** icon (top-right) → **New repository**
2. Fill in:
   - **Repository name**: `library-loan-system`
   - **Description**: `JDBC Library Loan Management System with Transaction Management & Performance Benchmarks using Apache Derby`
   - **Visibility**: Public (or Private — your choice)
   - ⚠️ **Do NOT** check "Add a README file" (you already have one)
   - ⚠️ **Do NOT** add .gitignore or license from GitHub (already have them)
3. Click **Create repository**

### 3.3 Copy the Repository URL
After creation, GitHub shows the repo URL. It looks like:
```
https://github.com/YOUR_USERNAME/library-loan-system.git
```

---

## PHASE 4 — Push to GitHub

### 4.1 Link Local Repo to GitHub
```bash
git remote add origin https://github.com/YOUR_USERNAME/library-loan-system.git
```
Replace `YOUR_USERNAME` with your actual GitHub username.

### 4.2 Push the Code
```bash
git branch -M main
git push -u origin main
```

Enter your GitHub username and password (or personal access token — see note below).

> **Note on Authentication**: GitHub no longer accepts passwords for HTTPS pushes.
> Create a Personal Access Token:
> GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token
> Give it `repo` scope. Use the token as your password.

### 4.3 Verify on GitHub
Open `https://github.com/YOUR_USERNAME/library-loan-system` in your browser.
You should see all files and your README rendered on the main page.

---

## PHASE 5 — Organize the Repository with GitHub Features

### 5.1 Add a Repository Description and Topics
1. On your repo page, click the ⚙️ gear icon next to **About** (right sidebar).
2. Add a description (copy from README or write your own).
3. Add topics (tags):
   ```
   java  jdbc  apache-derby  database  transactions  benchmarking  sql
   ```
4. Click **Save changes**.

### 5.2 Create a Release (Optional but Professional)
1. Click **Releases** → **Create a new release**
2. Tag: `v1.0.0`
3. Title: `v1.0.0 — Initial Release`
4. Description:
   ```markdown
   ## Features
   - Embedded Apache Derby database with auto-schema initialization
   - ACID-compliant loan processing with savepoints
   - 4 JDBC performance benchmark suites
   - Console-driven CLI with menu navigation
   - CSV performance report output
   ```
5. Click **Publish release**

### 5.3 Add a License (Optional)
1. Click **Add file** → **Create new file**
2. Name it `LICENSE`
3. Click **Choose a license template** → select **MIT License**
4. Fill in your name and year
5. Commit the file

---

## PHASE 6 — Ongoing Development Workflow

After the initial push, follow this workflow for every change:

### 6.1 Make Changes
Edit any `.java` file, `pom.xml`, or documentation.

### 6.2 Check What Changed
```bash
git status
git diff
```

### 6.3 Stage and Commit
```bash
# Stage specific files
git add src/main/java/com/library/benchmark/PerformanceEvaluator.java

# Or stage everything
git add .

# Commit with a meaningful message
git commit -m "feat: add warm-up phase to PerformanceEvaluator"
```

### 6.4 Push to GitHub
```bash
git push
```

---

## PHASE 7 — Good Commit Message Conventions

Use the following prefixes for clean commit history:

| Prefix | Use For |
|--------|---------|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `docs:` | Documentation changes |
| `refactor:` | Code restructuring, no behavior change |
| `test:` | Adding or updating tests |
| `perf:` | Performance improvements |
| `chore:` | Build system, dependencies |

Examples:
```bash
git commit -m "feat: add overdue loan notification query"
git commit -m "fix: correct savepoint rollback order in TransactionService"
git commit -m "perf: increase batch flush interval to 1000 rows"
git commit -m "docs: update analysis.md with benchmark results"
```

---

## PHASE 8 — Branch Strategy (For Larger Teams / Assignments)

```bash
# Create a feature branch
git checkout -b feature/add-fine-calculation

# Work on your changes, then commit
git add .
git commit -m "feat: add fine calculation for overdue loans"

# Push the branch
git push -u origin feature/add-fine-calculation

# On GitHub: open a Pull Request from feature/add-fine-calculation → main
# Review, then merge

# Back locally, update main
git checkout main
git pull
```

---

## PHASE 9 — Cloning & Running on Another Machine

Anyone with access to your repo can run the project with three commands:

```bash
# Clone
git clone https://github.com/YOUR_USERNAME/library-loan-system.git
cd library-loan-system

# Build
mvn clean package -q

# Run
java -jar target/library-loan-system.jar
```

---

## PHASE 10 — Troubleshooting

### "Repository not found" on push
- Verify the remote URL: `git remote -v`
- Fix it: `git remote set-url origin https://github.com/YOUR_USERNAME/library-loan-system.git`

### Authentication failed
- Use a Personal Access Token, not your GitHub password.
- Or configure SSH: https://docs.github.com/en/authentication/connecting-to-github-with-ssh

### "Class not found: org.apache.derby.jdbc.EmbeddedDriver"
- You forgot `mvn package`. Run it before `java -jar`.
- Or the `target/lib/` folder is missing — run `mvn clean package`.

### Derby database locked ("Another instance of Derby may have shut down abnormally")
- Delete the `librarydb/` folder and restart the application.

### Text block compile error
- Ensure you are using Java 17+: `java -version`
- If on Java 15/16, replace text blocks with regular String literals (remove triple-quote syntax).

---

## Quick Reference Card

```bash
# One-time setup
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOU/library-loan-system.git
git push -u origin main

# Daily workflow
git add .
git commit -m "feat: describe what you did"
git push

# Check history
git log --oneline

# Undo last commit (keep changes)
git reset --soft HEAD~1
```
