import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * InvoiceDAO handles persistence for checkout records in the `invoices` table.
 *
 * NOTE ON HISTORICAL BREAKDOWN:
 * The baseline `invoices` schema stores only `total_amount` (the grand total),
 * customer name, payment method, and timestamp. It does not include an invoice items
 * child table or separate subtotal/VAT columns. Detailed bill breakdown is computed
 * in memory at checkout time for display on the receipt.
 */
public class InvoiceDAO {

    /**
     * Model representing a persisted invoice record.
     */
    public static class Invoice {
        private final int invoiceId;
        private final String customerName;
        private final double totalAmount;
        private final String paymentMethod;
        private final Timestamp dateTime;

        public Invoice(int invoiceId, String customerName, double totalAmount, String paymentMethod, Timestamp dateTime) {
            this.invoiceId = invoiceId;
            this.customerName = customerName;
            this.totalAmount = totalAmount;
            this.paymentMethod = paymentMethod;
            this.dateTime = dateTime;
        }

        public int getInvoiceId() {
            return invoiceId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public Timestamp getDateTime() {
            return dateTime;
        }

        @Override
        public String toString() {
            return "Invoice #" + invoiceId + " | " + customerName + " | PHP " +
                    String.format("%.2f", totalAmount) + " | " + paymentMethod + " | " + dateTime;
        }
    }

    /**
     * Inserts a new invoice record within an active transaction connection.
     *
     * @param conn active JDBC Connection (with autoCommit=false)
     * @param customerName name of the customer
     * @param totalAmount grand total charged
     * @param paymentMethod payment method used
     * @return generated auto-increment invoice_id
     * @throws SQLException if insert fails or key generation fails
     */
    public int createInvoice(Connection conn, String customerName, double totalAmount, String paymentMethod) throws SQLException {
        String sql = "INSERT INTO invoices (customer_name, total_amount, payment_method, date_time) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, (customerName == null || customerName.trim().isEmpty()) ? "Walk-in Customer" : customerName.trim());
            stmt.setDouble(2, totalAmount);
            stmt.setString(3, paymentMethod);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating invoice failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating invoice failed, no generated ID obtained.");
                }
            }
        }
    }

    /**
     * Retrieves an invoice by primary key.
     */
    public Invoice getInvoiceById(int invoiceId) throws SQLException {
        String sql = "SELECT invoice_id, customer_name, total_amount, payment_method, date_time FROM invoices WHERE invoice_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, invoiceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Invoice(
                            rs.getInt("invoice_id"),
                            rs.getString("customer_name"),
                            rs.getDouble("total_amount"),
                            rs.getString("payment_method"),
                            rs.getTimestamp("date_time")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all recorded invoices ordered newest first.
     */
    public List<Invoice> getAllInvoices() throws SQLException {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT invoice_id, customer_name, total_amount, payment_method, date_time FROM invoices ORDER BY invoice_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Invoice(
                        rs.getInt("invoice_id"),
                        rs.getString("customer_name"),
                        rs.getDouble("total_amount"),
                        rs.getString("payment_method"),
                        rs.getTimestamp("date_time")
                ));
            }
        }
        return list;
    }
}

