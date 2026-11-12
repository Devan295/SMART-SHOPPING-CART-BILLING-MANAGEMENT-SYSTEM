# Smart Shopping Cart & Billing Management System

An enterprise-grade academic prototype of a **Smart Shopping Cart and Billing Management System** built using pure **Java AWT GUI**, **MySQL Connector/J (JDBC)**, and **MySQL Server 8.0**.

The application preserves the original Java AWT GUI design and layout while integrating complete database persistence, atomic inventory stock deduction, authoritative price retrieval, 12% VAT computation via `BigDecimal`, and transaction-safe checkout.

---

## 1. Project Overview & Mandatory Constraints

- **Language & Runtime:** Java (JDK 17+)
- **IDE:** IntelliJ IDEA
- **GUI Toolkit:** Pure Java AWT (`Frame`, `Dialog`, `List`, `Choice`, `TextField`, `Button`, `Label`, `TextArea`)
- **Database:** MySQL Server 8.0 (`smart_cart_db`)
- **Connectivity:** MySQL Connector/J JDBC Driver (`com.mysql.cj.jdbc.Driver`)
- **Build System:** Standard IntelliJ IDEA Java Project (**No Maven, No Gradle**)
- **Main Entry Point:** `src/Main.java`

---

## 2. Directory Structure

```text
SmartShoppingCart - Devanshi/
│
├── .idea/                               # IntelliJ IDEA workspace & run configurations
│   ├── runConfigurations/
│   │   ├── Main.xml                     # IntelliJ Run Configuration for Main
│   │   └── DBConnection.xml             # IntelliJ Run Configuration for DB Test
│   ├── misc.xml
│   └── modules.xml
│
├── bin/                                 # Compiled Java bytecode (.class files)
├── database/
│   ├── smart_cart_db.sql                # Baseline database schema and seed data
│   └── seed_sample_invoices.sql         # Optional sample invoice history
│
├── lib/
│   └── mysql-connector-j-26.10.0.jar    # Official MySQL JDBC Driver
│
├── src/                                 # Java Source Code (Default Package)
│   ├── Main.java                        # AWT GUI entry point & controller
│   ├── DBConnection.java                # Centralized JDBC connection manager & test
│   ├── UserDAO.java                     # User authentication DAO
│   ├── ProductDAO.java                  # Product catalog & inventory stock DAO
│   ├── CartDAO.java                     # Cart persistence DAO
│   ├── InvoiceDAO.java                  # Checkout & invoice records DAO
│   ├── CartItem.java                    # Cart item data model & row formatting
│   └── BillingService.java              # 12% VAT calculations & atomic checkout
│
├── test/
│   └── SystemVerificationTest.java      # Comprehensive automated verification suite
│
├── db.properties                        # Local MySQL credentials (git-ignored)
├── db.properties.example                # Sample database configuration template
├── SmartShoppingCart.iml                # IntelliJ IDEA module definition
├── run.bat                              # Windows batch launcher
├── run.ps1                              # PowerShell launcher
├── .gitignore                           # Git exclusion rules
└── README.md                            # Comprehensive project documentation
```

---

## 3. Database Schema (`smart_cart_db`)

The database uses the exact four tables specified in the baseline requirements:

```sql
CREATE DATABASE IF NOT EXISTS smart_cart_db;
USE smart_cart_db;

-- 1. User Accounts (Cashiers & Admins)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL
);

-- 2. Product Inventory
CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    price DOUBLE NOT NULL,
    category VARCHAR(50),
    stock_qty INT NOT NULL
);

-- 3. Persisted Shopping Cart
CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    price DOUBLE NOT NULL,
    quantity INT NOT NULL,
    total DOUBLE NOT NULL
);

-- 4. Invoices / Completed Orders
CREATE TABLE IF NOT EXISTS invoices (
    invoice_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100),
    total_amount DOUBLE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Preloaded Sample Accounts:
| Username | Password | Role / Description |
| :--- | :--- | :--- |
| `admin` | `1234` | Default Administrator |
| `cashier1` | `password11` | Store Cashier |
| `manager` | `admin99` | Store Manager |

### Preloaded Inventory Products:
| Product Name | Category | Unit Price (PHP) | Initial Stock |
| :--- | :--- | :--- | :--- |
| `Wireless Mouse` | Electronics | 450.00 | 50 |
| `USB Cable Type-C` | Electronics | 150.00 | 100 |
| `Coffee 200g` | Groceries | 180.00 | 40 |
| `Potato Chips 100g` | Groceries | 60.00 | 80 |
| `Notebook A5` | Stationery | 45.00 | 60 |

---

## 4. Database Setup Instructions

1. Ensure **MySQL Server 8.0** is installed and running on `localhost:3306`.
2. Open a terminal or MySQL Command Line Client:
   ```powershell
   Get-Content database\smart_cart_db.sql | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
   ```
   *(Enter your MySQL root password when prompted)*
3. Verify or update `db.properties` in the project root:
   ```properties
   db.url=jdbc:mysql://localhost:3306/smart_cart_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=your_mysql_password
   ```
   *Alternatively, set environment variables `DB_URL`, `DB_USER`, and `DB_PASSWORD`.*

---

## 5. Setting up in IntelliJ IDEA

### Step 1: Open the Project
1. Open **IntelliJ IDEA**.
2. Click **File** -> **Open...**
3. Select the folder `SmartShoppingCart - Devanshi` and click **OK**.
4. Trust the project if prompted.

### Step 2: Configure Project SDK
1. Go to **File** -> **Project Structure...** (or press `Ctrl + Alt + Shift + S`).
2. Under **Project Settings** -> **Project**:
   - Set **SDK** to your installed JDK (JDK 17, 21, or 27).
   - Set **Language level** to `17` or `SDK default`.

### Step 3: Verify the MySQL Connector/J Library
1. In **Project Structure**, go to **Modules** -> **Dependencies** tab.
2. Verify that `mysql-connector-j-26.10.0.jar` is listed.
3. If you ever need to re-add it manually:
   - Click the `+` button -> **JARs or Directories...**
   - Browse to `lib/mysql-connector-j-26.10.0.jar`
   - Select it, choose **Compile**, and click **Apply** / **OK**.

### Step 4: Run the Application
1. In the top toolbar, select the pre-configured run configuration **Main**.
2. Click the green **Run (Play)** button (or press `Shift + F10`).
3. To test database connectivity independently, select **DBConnection (Test)** and run it.

---

## 6. Running from the Command Line / PowerShell

You can compile and run the application without IntelliJ IDEA using the included scripts:

### Using the Batch Script (Command Prompt):
```cmd
run.bat
```

### Using PowerShell:
```powershell
.\run.ps1
```

### Manual Compilation & Execution:
```powershell
# Compile
javac -cp "lib/*" -d bin src/*.java

# Run Application
java -cp "bin;lib/*" Main

# Test Database Connection Independently
java -cp "bin;lib/*" DBConnection
```

---

## 7. Key Features & Architectural Improvements

### 1. Authoritative Pricing & Inventory Validation
- Manual typing of lower prices is thwarted: When a product is selected or typed, `ProductDAO` fetches the authoritative price and current stock from the database.
- Non-existent products, zero, or negative quantities are rejected with clear error prompts.
- Out-of-stock items cannot be added to the cart.

### 2. Live Cart Persistence & Row Mapping
- Cart items are stored directly in MySQL `cart_items`.
- Each cart row maps directly to a `CartItem` with its database `cart_item_id`.
- Modifying or removing an item in the GUI directly updates or deletes the matching row in MySQL.
- Adding an already existing item intelligently merges quantities and recalculates line totals.

### 3. High-Precision Financial Computation
- Computations use `java.math.BigDecimal` with `RoundingMode.HALF_UP` to prevent floating-point inaccuracies.
- Tax policy preserves **12% VAT** (`Subtotal * 0.12`).
- Grand Total = `Subtotal + 12% VAT`.

### 4. Transaction-Safe Checkout
- Checkout executes within an atomic database transaction (`conn.setAutoCommit(false)`).
- Atomically verifies stock levels and conditionally decrements inventory (`UPDATE products SET stock_qty = stock_qty - ? WHERE product_id = ? AND stock_qty >= ?`).
- Inserts an invoice into the `invoices` table and retrieves the auto-generated `invoice_id`.
- Clears the `cart_items` table only after the invoice and stock updates succeed.
- If any item has insufficient stock or a failure occurs, the entire transaction **rolls back** and the cart remains completely intact.

### 5. Official Receipt Dialog
- On successful checkout, an AWT modal `ReceiptDialog` displays an itemized ASCII receipt with Invoice ID, Timestamp, Customer Name, Payment Method, Item Breakdown, Subtotal, 12% VAT, and Grand Total.

---

## 8. Architectural & Security Notes

> [!NOTE]
> **Academic Prototype Password Storage:**
> The baseline database schema uses plaintext passwords to maintain 100% compatibility with the original project requirements and seed records. For production deployments, passwords must be securely hashed using salted algorithms (e.g., BCrypt or Argon2) and column widths expanded accordingly.

> [!NOTE]
> **Shared Prototype Cart:**
> As specified in the baseline schema, the `cart_items` table does not contain a session token or cashier ID column. Consequently, it represents a single shared prototype cart. Multi-cashier concurrency would require an isolated session identifier or user-scoped cart table.

> [!NOTE]
> **Invoice Historical Granularity:**
> The baseline `invoices` table stores only `total_amount` (grand total), customer name, payment method, and timestamp. Itemized breakdown is displayed and printed dynamically at checkout time. Full historical item reporting would require a separate `invoice_items` child table.

---

## 9. Verification & Automated Test Suite

An automated end-to-end verification suite is included at `test/SystemVerificationTest.java`. It tests:
1. **Database Connectivity:** Direct ping to `smart_cart_db`.
2. **User Authentication:** Validates `admin`, `cashier1`, `manager`, and rejection of bad credentials.
3. **Product Inventory:** Verifies authoritative pricing, keyword search, and stock queries.
4. **Cart Operations:** Validates add, merge, update, delete, line total calculations, and clearing.
5. **Billing & Atomic Checkout:** Validates 12% VAT math, transaction rollback upon insufficient stock, stock deduction, and invoice generation.

To run the verification test suite:
```powershell
javac -cp "lib/*;bin" -d bin test/SystemVerificationTest.java
java -cp "bin;lib/*" SystemVerificationTest
```
*(Result: 45/45 assertions pass with 0 failures)*

