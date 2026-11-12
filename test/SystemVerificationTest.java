import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Automated Verification Test for Smart Shopping Cart & Billing Management System.
 * Tests all requirements, DAOs, transaction rollback, and billing calculations.
 */
public class SystemVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + testName);
            testsFailed++;
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        if (expected == null && actual == null) {
            System.out.println("  [PASS] " + testName);
            testsPassed++;
        } else if (expected != null && expected.equals(actual)) {
            System.out.println("  [PASS] " + testName);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + testName + " - Expected: <" + expected + ">, Got: <" + actual + ">");
            testsFailed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println(" SMART SHOPPING CART SYSTEM - AUTOMATED VERIFICATION SUITE    ");
        System.out.println("===============================================================\n");

        testDatabaseConnectivity();
        testUserAuthentication();
        testProductInventory();
        testCartOperations();
        testBillingCalculationsAndTransaction();

        System.out.println("\n===============================================================");
        System.out.printf(" VERIFICATION SUMMARY: Passed: %d | Failed: %d\n", testsPassed, testsFailed);
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void testDatabaseConnectivity() {
        System.out.println("--- 1. Testing Database Connectivity (DBConnection) ---");
        boolean connected = DBConnection.testConnection();
        assertTrue("DBConnection successfully established to smart_cart_db", connected);
    }

    private static void testUserAuthentication() {
        System.out.println("\n--- 2. Testing User Authentication (UserDAO) ---");
        UserDAO userDAO = new UserDAO();
        try {
            UserDAO.User admin = userDAO.authenticate("admin", "1234");
            assertTrue("Admin credentials validated ('admin' / '1234')", admin != null && "admin".equals(admin.getUsername()));

            UserDAO.User cashier = userDAO.authenticate("cashier1", "password11");
            assertTrue("Cashier credentials validated ('cashier1' / 'password11')", cashier != null && "cashier1".equals(cashier.getUsername()));

            UserDAO.User manager = userDAO.authenticate("manager", "admin99");
            assertTrue("Manager credentials validated ('manager' / 'admin99')", manager != null && "manager".equals(manager.getUsername()));

            UserDAO.User badUser = userDAO.authenticate("nonexistent", "wrongpass");
            assertTrue("Non-existent user rejected", badUser == null);

            UserDAO.User wrongPass = userDAO.authenticate("admin", "wrongpassword");
            assertTrue("Incorrect password rejected", wrongPass == null);

        } catch (SQLException e) {
            assertTrue("UserDAO SQL Exception: " + e.getMessage(), false);
        }
    }

    private static void testProductInventory() {
        System.out.println("\n--- 3. Testing Product Catalog & Stock (ProductDAO) ---");
        ProductDAO productDAO = new ProductDAO();
        try {
            List<ProductDAO.Product> products = productDAO.getAllProducts();
            assertTrue("Catalog loaded with products", products != null && !products.isEmpty());

            ProductDAO.Product mouse = productDAO.getProductByName("Wireless Mouse");
            assertTrue("Find product by exact name 'Wireless Mouse'", mouse != null);
            if (mouse != null) {
                assertEquals("Wireless Mouse price is 450.00", 450.00, mouse.getPrice());
                assertTrue("Wireless Mouse stock > 0", mouse.getStockQty() > 0);
            }

            List<ProductDAO.Product> searchResults = productDAO.searchProducts("USB");
            assertTrue("Search products with keyword 'USB' found match", !searchResults.isEmpty());

            ProductDAO.Product nonExistent = productDAO.getProductByName("Super Invisible Widget 9000");
            assertTrue("Unknown product returns null", nonExistent == null);

        } catch (SQLException e) {
            assertTrue("ProductDAO SQL Exception: " + e.getMessage(), false);
        }
    }

    private static void testCartOperations() {
        System.out.println("\n--- 4. Testing Cart Operations (CartDAO & CartItem) ---");
        CartDAO cartDAO = new CartDAO();
        try {
            // Start fresh
            cartDAO.clearCart();
            List<CartItem> initialCart = cartDAO.getAllCartItems();
            assertEquals("Cart is initially empty after clearCart()", 0, initialCart.size());

            // Add new item
            CartItem item1 = new CartItem("Wireless Mouse", 450.00, 2);
            boolean added1 = cartDAO.addOrMergeItem(item1);
            assertTrue("Added 'Wireless Mouse' x2 to cart", added1);

            List<CartItem> cartAfterAdd = cartDAO.getAllCartItems();
            assertEquals("Cart has 1 item", 1, cartAfterAdd.size());
            assertEquals("Item total calculated correctly", 900.00, cartAfterAdd.get(0).getTotal());

            // Add same item again (should merge quantity)
            CartItem item1More = new CartItem("Wireless Mouse", 450.00, 1);
            cartDAO.addOrMergeItem(item1More);
            List<CartItem> cartAfterMerge = cartDAO.getAllCartItems();
            assertEquals("Cart still has 1 merged item row", 1, cartAfterMerge.size());
            assertEquals("Merged quantity is 3", 3, cartAfterMerge.get(0).getQuantity());
            assertEquals("Merged total is 1350.00", 1350.00, cartAfterMerge.get(0).getTotal());

            // Update item
            CartItem toUpdate = cartAfterMerge.get(0);
            toUpdate.setQuantity(1);
            toUpdate.recalculateTotal();
            boolean updated = cartDAO.updateCartItem(toUpdate);
            assertTrue("Updated item quantity to 1", updated);
            assertEquals("Updated total is 450.00", 450.00, cartDAO.getCartItemById(toUpdate.getCartItemId()).getTotal());

            // Delete item
            boolean deleted = cartDAO.deleteCartItem(toUpdate.getCartItemId());
            assertTrue("Deleted item from cart", deleted);
            assertEquals("Cart is empty after delete", 0, cartDAO.getAllCartItems().size());

        } catch (SQLException e) {
            assertTrue("CartDAO SQL Exception: " + e.getMessage(), false);
        }
    }

    private static void testBillingCalculationsAndTransaction() {
        System.out.println("\n--- 5. Testing Billing Calculations & Atomic Checkout (BillingService) ---");
        ProductDAO productDAO = new ProductDAO();
        CartDAO cartDAO = new CartDAO();
        InvoiceDAO invoiceDAO = new InvoiceDAO();
        BillingService billingService = new BillingService(productDAO, cartDAO, invoiceDAO);

        try {
            cartDAO.clearCart();

            // Populate cart with known test items
            // Wireless Mouse: price 450.00, qty 1 -> 450.00
            // Notebook A5: price 45.00, qty 2 -> 90.00
            cartDAO.addOrMergeItem(new CartItem("Wireless Mouse", 450.00, 1));
            cartDAO.addOrMergeItem(new CartItem("Notebook A5", 45.00, 2));

            List<CartItem> cartItems = cartDAO.getAllCartItems();
            BigDecimal subtotal = billingService.calculateSubtotal(cartItems);
            BigDecimal tax = billingService.calculateTax(subtotal);
            BigDecimal grandTotal = billingService.calculateGrandTotal(subtotal, tax);

            // Subtotal: 450 + 90 = 540.00
            // Tax: 540 * 0.12 = 64.80
            // Grand Total: 540 + 64.80 = 604.80
            assertEquals("Subtotal matches expected 540.00", new BigDecimal("540.00"), subtotal);
            assertEquals("12% VAT matches expected 64.80", new BigDecimal("64.80"), tax);
            assertEquals("Grand total matches expected 604.80", new BigDecimal("604.80"), grandTotal);

            // Record initial stock for verification
            ProductDAO.Product mouseBefore = productDAO.getProductByName("Wireless Mouse");
            ProductDAO.Product notebookBefore = productDAO.getProductByName("Notebook A5");
            int mouseStockBefore = mouseBefore.getStockQty();
            int notebookStockBefore = notebookBefore.getStockQty();

            // Test Rollback: Add an item with excessive quantity beyond stock
            cartDAO.addOrMergeItem(new CartItem("Potato Chips 100g", 60.00, 999999));
            boolean rollbackCaught = false;
            try {
                billingService.processCheckout("Rollback Test Customer", "Cash");
            } catch (IllegalStateException e) {
                rollbackCaught = true;
                System.out.println("  [PASS] Excessive quantity correctly prevented checkout and triggered rollback: " + e.getMessage());
                testsPassed++;
            }
            assertTrue("Checkout failed cleanly when inventory was insufficient", rollbackCaught);

            // Verify cart items were preserved during rollback!
            List<CartItem> cartAfterFailedCheckout = cartDAO.getAllCartItems();
            assertTrue("Cart items preserved after failed checkout rollback", cartAfterFailedCheckout.size() == 3);

            // Remove the excessive item to proceed with valid checkout
            CartItem excessiveItem = cartDAO.getCartItemByName("Potato Chips 100g");
            if (excessiveItem != null) {
                cartDAO.deleteCartItem(excessiveItem.getCartItemId());
            }

            // Perform valid checkout
            BillingService.CheckoutResult result = billingService.processCheckout("Maria Clara", "GCash / E-Wallet");
            assertTrue("Checkout completed and generated positive invoice ID", result.getInvoiceId() > 0);
            assertEquals("Result customer name matches", "Maria Clara", result.getCustomerName());
            assertEquals("Result payment method matches", "GCash / E-Wallet", result.getPaymentMethod());
            assertEquals("Result grand total matches", new BigDecimal("604.80"), result.getGrandTotal());

            // Verify cart was cleared after successful checkout
            List<CartItem> cartAfterSuccess = cartDAO.getAllCartItems();
            assertEquals("Cart is cleared after successful checkout", 0, cartAfterSuccess.size());

            // Verify inventory stock was reduced accurately
            ProductDAO.Product mouseAfter = productDAO.getProductByName("Wireless Mouse");
            ProductDAO.Product notebookAfter = productDAO.getProductByName("Notebook A5");
            assertEquals("Wireless Mouse stock decremented by 1", mouseStockBefore - 1, mouseAfter.getStockQty());
            assertEquals("Notebook A5 stock decremented by 2", notebookStockBefore - 2, notebookAfter.getStockQty());

            // Verify invoice is saved in database
            InvoiceDAO.Invoice persistedInvoice = invoiceDAO.getInvoiceById(result.getInvoiceId());
            assertTrue("Invoice record persisted in invoices table", persistedInvoice != null);
            if (persistedInvoice != null) {
                assertEquals("Persisted invoice customer matches", "Maria Clara", persistedInvoice.getCustomerName());
                assertEquals("Persisted invoice total matches", 604.80, persistedInvoice.getTotalAmount());
                assertEquals("Persisted payment method matches", "GCash / E-Wallet", persistedInvoice.getPaymentMethod());
            }

            // Verify receipt string formatting
            String receipt = result.generateReceiptText();
            assertTrue("Receipt contains header", receipt.contains("OFFICIAL RECEIPT"));
            assertTrue("Receipt contains Invoice ID", receipt.contains("Invoice ID   : #" + result.getInvoiceId()));
            assertTrue("Receipt contains Wireless Mouse", receipt.contains("Wireless Mouse"));
            assertTrue("Receipt contains 12% VAT", receipt.contains("Tax (12% VAT PHP)"));
            assertTrue("Receipt contains Grand Total", receipt.contains("604.80"));

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("BillingService Exception: " + e.getMessage(), false);
        }
    }
}

