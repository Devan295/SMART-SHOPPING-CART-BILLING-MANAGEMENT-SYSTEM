CREATE DATABASE IF NOT EXISTS smart_cart_db;
USE smart_cart_db;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    price DOUBLE NOT NULL,
    category VARCHAR(50),
    stock_qty INT NOT NULL
);

CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    price DOUBLE NOT NULL,
    quantity INT NOT NULL,
    total DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS invoices (
    invoice_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100),
    total_amount DOUBLE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (username, password)
SELECT 'admin', '1234'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

INSERT INTO users (username, password)
SELECT 'cashier1', 'password11'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'cashier1'
);

INSERT INTO users (username, password)
SELECT 'manager', 'admin99'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'manager'
);

INSERT INTO products (product_name, price, category, stock_qty)
SELECT 'Wireless Mouse', 450.00, 'Electronics', 50
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE product_name = 'Wireless Mouse'
);

INSERT INTO products (product_name, price, category, stock_qty)
SELECT 'USB Cable Type-C', 150.00, 'Electronics', 100
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE product_name = 'USB Cable Type-C'
);

INSERT INTO products (product_name, price, category, stock_qty)
SELECT 'Coffee 200g', 180.00, 'Groceries', 40
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE product_name = 'Coffee 200g'
);

INSERT INTO products (product_name, price, category, stock_qty)
SELECT 'Potato Chips 100g', 60.00, 'Groceries', 80
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE product_name = 'Potato Chips 100g'
);

INSERT INTO products (product_name, price, category, stock_qty)
SELECT 'Notebook A5', 45.00, 'Stationery', 60
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE product_name = 'Notebook A5'
);

SELECT * FROM users;
SELECT * FROM products;
SELECT * FROM cart_items;
SELECT * FROM invoices;

