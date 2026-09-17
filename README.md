# Bank Statement Service & Ledger Portal

A full-stack banking statement generation and ledger management system built with **Spring Boot 3 (Java 17)**, **H2 Database**, **OpenPDF**, and a **Vanilla Web Dashboard**.

The service handles transaction aggregation, dynamic fee and tiered interest calculation, scheduled monthly statement finalization, password-protected PDF generation, and an interactive customer & admin dashboard.

---

## 🌟 Key Features

- **Multi-Account Types**: Full financial modeling for `SAVINGS`, `CREDIT`, and `WALLET` accounts with distinct ledger calculations.
- **Statement Generation Engine**:
  - **PROVISIONAL Statements**: Real-time aggregation of the current in-progress month, computing live daily balances, provisional fees, and accrued interest.
  - **GENERATED Statements**: Finalized, immutable statements for past months persisted to the database.
- **Automated Monthly Batch Job**:
  - Automatically finalizes previous month statements at midnight on the 1st of every month (`0 0 0 1 * *`).
  - Idempotent execution with per-account isolation—failures on one account do not abort batch processing for others.
  - Manual trigger available for Administrators via REST API and the dashboard.
- **Dynamic Fee & Interest Engines**:
  - **Fees**: Monthly account maintenance, minimum average balance penalty, excess withdrawal fees, and negative balance/overdraft fees.
  - **Interest**: Day-weighted tiered positive interest on savings balances and debit interest charged on negative/credit balances.
- **Password-Protected PDF Export**:
  - Thymeleaf + OpenPDF rendering compliant bank statement documents.
  - 128-bit encryption locked with an account-holder password rule (`FIRST4NAME + LAST4ACCOUNT`).
  - Efficient file-system caching for finalized statements (`./data/statement-pdfs/`).
- **Security & RBAC**:
  - Stateless JWT (JSON Web Token) authentication with BCrypt password hashing.
  - Role-Based Access Control (`ROLE_USER`, `ROLE_ADMIN`) with strict IDOR protections (users cannot view other users' accounts).
- **Interactive Web Dashboard**:
  - Modern, responsive UI with month-by-month history sidebar, balance breakdown summary, transaction ledger, and PDF download.
  - Integrated Admin control panel for running batch statements on demand.

---

## 🏗️ Project Architecture

```
bank_statement/
├── frontend/                     # Lightweight frontend client (HTML5 / Vanilla JS / CSS)
│   ├── index.html                # Customer & Admin login interface
│   ├── dashboard.html            # Core banking portal & statement viewer
│   ├── dashboard.js              # State management, API calls, and PDF download
│   ├── login.js                  # Authentication & token storage
│   └── styles.css                # Polished design system
├── src/
│   ├── main/
│   │   ├── java/com/bank/statements/
│   │   │   ├── config/           # Security, scheduling, and configuration properties
│   │   │   ├── controller/       # REST API endpoints (Auth, Accounts, Statements, Admin)
│   │   │   ├── dto/              # Request/Response data transfer objects
│   │   │   ├── exception/        # Centralized exception handling & error responses
│   │   │   ├── model/            # JPA entities (User, Account, Transaction, Fee, Statement, etc.)
│   │   │   ├── repository/       # Spring Data JPA repositories
│   │   │   ├── security/         # JWT filter, UserDetailsService, and Auth entry points
│   │   │   └── service/          # Core business services:
│   │   │       ├── balance/      # Daily balance tracking & average balance computation
│   │   │       ├── batch/        # Monthly statement generation batch job
│   │   │       ├── fee/          # Extensible fee rules engine
│   │   │       ├── interest/     # Tiered interest calculation engine
│   │   │       ├── pdf/          # PDF rendering & password encryption service
│   │   │       └── statement/    # Statement synthesis and aggregation
│   │   └── resources/
│   │       ├── application.yml   # Primary application configuration
│   │       ├── application-dev.yml # Dev profile with H2 web console enabled
│   │       └── templates/
│   │           └── statement.html # Thymeleaf PDF template
│   └── test/                     # Unit and integration test suites
├── data/                         # Local runtime storage (ignored by git)
│   ├── bankstatement.mv.db       # H2 file-based persistent database
│   └── statement-pdfs/           # Cached PDF statements
├── .gitignore                    # Git rules for build artifacts & runtime data
└── pom.xml                       # Maven build specification
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 17** or higher
- **Maven 3.8+**
- Modern Web Browser

### 1. Start the Backend API

Run the application using Maven:

```bash
mvn spring-boot:run
```

The server will start on **`http://localhost:8080`**.

> **Note**: Seed data is automatically loaded on the very first boot. The database persists to `./data/bankstatement.mv.db`.

#### Enabling H2 Web Console (Local Dev Only)
To inspect the database tables directly:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Access the console at: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:file:./data/bankstatement`
- **User Name**: `sa`
- **Password**: *(leave blank)*

---

### 2. Launch the Frontend

The frontend is pure static HTML/CSS/JS with zero build dependencies.

You can serve it with Python:
```bash
cd frontend
python -m http.server 5500
```
Then visit: **`http://localhost:5500`** *(or simply double-click `frontend/index.html` to open it in your browser)*.

---

## 👥 Seeded Demo Credentials

All test users share the password: **`password123`**

| Name | Email | Role | Seeded Accounts |
| :--- | :--- | :--- | :--- |
| **Asha Rao** | `asha.rao@example.com` | `USER` | `SAV-0001-0001` (Savings, INR)<br>`CRD-0001-0002` (Credit, INR) |
| **Vikram Shah** | `vikram.shah@example.com` | `USER` | `WAL-0002-0003` (Wallet, INR)<br>`SAV-0002-0004` (Savings, Zero-activity) |
| **Admin User** | `admin@example.com` | `ADMIN` | Full access to execute statement batch runs |

---

## 🔒 PDF Password Rules

All downloaded statement PDFs are encrypted with standard 128-bit encryption.

- **Formula**: `FIRST_4_LETTERS_OF_NAME (UPPERCASE) + LAST_4_DIGITS_OF_ACCOUNT`
- **Examples**:
  - **Asha Rao** with account `SAV-0001-0001` ➔ Password: **`ASHA0001`**
  - **Vikram Shah** with account `WAL-0002-0003` ➔ Password: **`VIKR0003`**

---

## 📡 REST API Reference

All requests outside `/api/auth/**` require the header:  
`Authorization: Bearer <JWT_TOKEN>`

### Authentication
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Public | Authenticate user and receive JWT bearer token |

### Accounts
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accounts` | Authenticated | List all accounts owned by current user |
| `GET` | `/api/accounts/{accountId}` | Authenticated | Get detailed summary for a specific account |

### Statements
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accounts/{accountId}/statements` | Authenticated | List generated statement history for an account |
| `GET` | `/api/accounts/{accountId}/statements/{year}/{month}` | Authenticated | Fetch JSON statement details (generated or provisional) |
| `GET` | `/api/accounts/{accountId}/statements/{year}/{month}/pdf` | Authenticated | Download password-protected PDF statement |

### Admin Batch Operations
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/admin/batch/run` | `ADMIN` | Trigger batch statement generation for a specific month |

**Example Admin Batch Request Body:**
```json
{
  "year": 2026,
  "month": 8
}
```

---

## ⚙️ Configuration Reference (`application.yml`)

Key settings can be modified in `src/main/resources/application.yml`:

```yaml
bank:
  batch:
    # 6-field Spring cron: second minute hour day month day-of-week
    # Default: 1st of every month at midnight (12:00 AM)
    cron: "0 0 0 1 * *"
  jwt:
    secret: "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    expiration-ms: 3600000 # 1 hour
  pdf:
    storage-dir: "./data/statement-pdfs"
  fee:
    maintenance-fee: 150.00
    minimum-balance: 10000.00
    minimum-balance-fee: 300.00
    excess-withdrawal-fee: 50.00
    overdraft-fee: 500.00
  interest:
    savings-rate: 0.0350   # 3.5% base
    credit-rate: 0.4200    # 42% APR
```

---

## 🧪 Testing & Verification

Run the test suite via Maven:

```bash
mvn test
```

Build an executable production JAR:

```bash
mvn clean package
java -jar target/bank-statement-service-0.0.1-SNAPSHOT.jar
```
