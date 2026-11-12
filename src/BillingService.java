import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BillingService handles financial calculations, tax computation (12% VAT),
 * and transaction-safe checkout workflows.
 */
public class BillingService {

    public static final BigDecimal VAT_RATE = new BigDecimal("0.12");

    private final ProductDAO productDAO;
    private final CartDAO cartDAO;
    private final InvoiceDAO invoiceDAO;

    public BillingService() {
        this(new ProductDAO(), new CartDAO(), new InvoiceDAO());
    }

    public BillingService(ProductDAO productDAO, CartDAO cartDAO, InvoiceDAO invoiceDAO) {
        this.productDAO = productDAO;
        this.cartDAO = cartDAO;
        this.invoiceDAO = invoiceDAO;
    }

    /**
     * Calculates the cart subtotal using BigDecimal for monetary accuracy.
     */
    public BigDecimal calculateSubtotal(List<CartItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (items == null) return subtotal;

        for (CartItem item : items) {
            BigDecimal price = BigDecimal.valueOf(item.getPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            subtotal = subtotal.add(price.multiply(qty));
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates 12% VAT from the subtotal.
     */
    public BigDecimal calculateTax(BigDecimal subtotal) {
        if (subtotal == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Grand Total (Subtotal + VAT).
     */
    public BigDecimal calculateGrandTotal(BigDecimal subtotal, BigDecimal tax) {
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (tax == null) tax = BigDecimal.ZERO;
        return subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Executes the checkout process atomically within a database transaction.
     *
     * Steps:
     * 1. Validate cart contains items.
     * 2. Validate customer name and payment method.
     * 3. Compute subtotal, 12% VAT, and grand total.
     * 4. Open transaction (`conn.setAutoCommit(false)`).
     * 5. Atomically check stock and deduct from `products` table.
     * 6. Insert new record into `invoices` table.
     * 7. Clear `cart_items` table.
     * 8. Commit transaction.
     * On error, all database modifications rollback and cart items are preserved.
     *
     * @param customerName name of customer
     * @param paymentMethod selected payment method
     * @return CheckoutResult containing receipt details
     * @throws SQLException or IllegalStateException on validation or database failure
     */
    public CheckoutResult processCheckout(String customerName, String paymentMethod) throws SQLException, IllegalStateException {
        List<CartItem> currentCart = cartDAO.getAllCartItems();
        if (currentCart.isEmpty()) {
            throw new IllegalStateException("Cart is empty! Please add items before checkout.");
        }

        String safeCustomer = (customerName == null || customerName.trim().isEmpty())
                ? "Walk-in Customer" : customerName.trim();

        String safePayment = (paymentMethod == null || paymentMethod.trim().isEmpty())
                ? "Cash" : paymentMethod.trim();

        // Calculate monetary totals
        BigDecimal subtotal = calculateSubtotal(currentCart);
        BigDecimal tax = calculateTax(subtotal);
        BigDecimal grandTotal = calculateGrandTotal(subtotal, tax);

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Deduct stock for each cart item conditionally
            for (CartItem item : currentCart) {
                boolean deducted = productDAO.deductStockInTransaction(conn, item.getProductName(), item.getQuantity());
                if (!deducted) {
                    throw new IllegalStateException("Insufficient inventory for '" + item.getProductName() +
                            "'. Available stock is less than " + item.getQuantity() + " or product was not found.");
                }
            }

            // 2. Insert invoice record
            int invoiceId = invoiceDAO.createInvoice(conn, safeCustomer, grandTotal.doubleValue(), safePayment);

            // 3. Clear cart table
            cartDAO.clearCart(conn);

            // 4. Commit transaction
            conn.commit();

            Timestamp now = new Timestamp(System.currentTimeMillis());
            return new CheckoutResult(invoiceId, safeCustomer, safePayment, currentCart, subtotal, tax, grandTotal, now);

        } catch (Exception ex) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction on failure
                } catch (SQLException rollbackEx) {
                    System.err.println("[BillingService] Error during transaction rollback: " + rollbackEx.getMessage());
                }
            }
            if (ex instanceof IllegalStateException) {
                throw (IllegalStateException) ex;
            } else if (ex instanceof SQLException) {
                throw (SQLException) ex;
            } else {
                throw new SQLException("Checkout failed: " + ex.getMessage(), ex);
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * DTO encapsulating the receipt data of a completed checkout.
     */
    public static class CheckoutResult {
        private final int invoiceId;
        private final String customerName;
        private final String paymentMethod;
        private final List<CartItem> purchasedItems;
        private final BigDecimal subtotal;
        private final BigDecimal tax;
        private final BigDecimal grandTotal;
        private final Timestamp dateTime;

        public CheckoutResult(int invoiceId, String customerName, String paymentMethod,
                              List<CartItem> purchasedItems, BigDecimal subtotal,
                              BigDecimal tax, BigDecimal grandTotal, Timestamp dateTime) {
            this.invoiceId = invoiceId;
            this.customerName = customerName;
            this.paymentMethod = paymentMethod;
            this.purchasedItems = Collections.unmodifiableList(new ArrayList<>(purchasedItems));
            this.subtotal = subtotal;
            this.tax = tax;
            this.grandTotal = grandTotal;
            this.dateTime = dateTime;
        }

        public int getInvoiceId() {
            return invoiceId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public List<CartItem> getPurchasedItems() {
            return purchasedItems;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public BigDecimal getTax() {
            return tax;
        }

        public BigDecimal getGrandTotal() {
            return grandTotal;
        }

        public Timestamp getDateTime() {
            return dateTime;
        }

        /**
         * Formats an itemized receipt suitable for display or printing.
         */
        public String generateReceiptText() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            StringBuilder sb = new StringBuilder();
            sb.append("====================================================\n");
            sb.append("          SMART SHOPPING CART & BILLING             \n");
            sb.append("                 OFFICIAL RECEIPT                   \n");
            sb.append("====================================================\n");
            sb.append(String.format("Invoice ID   : #%d\n", invoiceId));
            sb.append(String.format("Date / Time  : %s\n", sdf.format(dateTime)));
            sb.append(String.format("Customer     : %s\n", customerName));
            sb.append(String.format("Payment Mode : %s\n", paymentMethod));
            sb.append("----------------------------------------------------\n");
            sb.append(String.format("%-4s %-20s %-10s %-5s %-10s\n", "No.", "Item Name", "Price", "Qty", "Total"));
            sb.append("----------------------------------------------------\n");

            int idx = 1;
            for (CartItem item : purchasedItems) {
                String name = item.getProductName();
                if (name.length() > 20) {
                    name = name.substring(0, 17) + "...";
                }
                sb.append(String.format("%-4d %-20s %-10.2f %-5d %-10.2f\n",
                        idx++, name, item.getPrice(), item.getQuantity(), item.getTotal()));
            }

            sb.append("----------------------------------------------------\n");
            sb.append(String.format("Subtotal (PHP)          : %18.2f\n", subtotal));
            sb.append(String.format("Tax (12%% VAT PHP)       : %18.2f\n", tax));
            sb.append(String.format("Grand Total (PHP)       : %18.2f\n", grandTotal));
            sb.append("====================================================\n");
            sb.append("       Thank you for shopping with us!              \n");
            sb.append("====================================================\n");
            return sb.toString();
        }
    }
}

