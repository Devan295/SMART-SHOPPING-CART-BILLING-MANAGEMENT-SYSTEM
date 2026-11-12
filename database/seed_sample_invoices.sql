-- Optional Seed Script: Sample Invoice History
-- Use this script if you wish to populate historical invoice demo data.
USE smart_cart_db;

INSERT INTO invoices (customer_name, total_amount, payment_method)
SELECT 'Walk-in Customer', 1041.60, 'Cash'
WHERE NOT EXISTS (SELECT 1 FROM invoices WHERE customer_name = 'Walk-in Customer' AND total_amount = 1041.60);

INSERT INTO invoices (customer_name, total_amount, payment_method)
SELECT 'Juan Dela Cruz', 504.00, 'GCash / E-Wallet'
WHERE NOT EXISTS (SELECT 1 FROM invoices WHERE customer_name = 'Juan Dela Cruz' AND total_amount = 504.00);

INSERT INTO invoices (customer_name, total_amount, payment_method)
SELECT 'Maria Santos', 168.00, 'Credit/Debit Card'
WHERE NOT EXISTS (SELECT 1 FROM invoices WHERE customer_name = 'Maria Santos' AND total_amount = 168.00);

