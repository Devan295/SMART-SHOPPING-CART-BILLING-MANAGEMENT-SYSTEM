import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductDAO manages inventory operations on the `products` table.
 *
 * Responsibilities:
 * - Load catalog products and stock quantities.
 * - Retrieve authoritative product prices.
 * - Validate inventory levels.
 * - Perform atomic stock deductions during checkout transactions.
 */
public class ProductDAO {

    /**
     * Model class representing a product record.
     */
    public static class Product {
        private final int productId;
        private final String productName;
        private final double price;
        private final String category;
        private final int stockQty;

        public Product(int productId, String productName, double price, String category, int stockQty) {
            this.productId = productId;
            this.productName = productName;
            this.price = price;
            this.category = category;
            this.stockQty = stockQty;
        }

        public int getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public double getPrice() {
            return price;
        }

        public String getCategory() {
            return category;
        }

        public int getStockQty() {
            return stockQty;
        }

        @Override
        public String toString() {
            return productName + " - PHP " + String.format("%.2f", price) + " (Stock: " + stockQty + ")";
        }
    }

    /**
     * Retrieves all products ordered by product name.
     */
    public List<Product> getAllProducts() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT product_id, product_name, price, category, stock_qty FROM products ORDER BY product_name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Finds a product by exact or case-insensitive product name.
     */
    public Product getProductByName(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT product_id, product_name, price, category, stock_qty FROM products WHERE LOWER(product_name) = LOWER(?) LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a product by its primary key.
     */
    public Product getProductById(int productId) throws SQLException {
        String sql = "SELECT product_id, product_name, price, category, stock_qty FROM products WHERE product_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Searches products containing the given keyword in their name or category.
     */
    public List<Product> searchProducts(String keyword) throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT product_id, product_name, price, category, stock_qty FROM products " +
                "WHERE LOWER(product_name) LIKE ? OR LOWER(category) LIKE ? ORDER BY product_name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String pattern = "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves current stock quantity for a given product ID.
     */
    public int getStock(int productId) throws SQLException {
        String sql = "SELECT stock_qty FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock_qty");
                }
            }
        }
        return 0;
    }

    /**
     * Conditionally reduces stock for a product inside an existing transaction.
     * Prevents stock from becoming negative by verifying `stock_qty >= deductQty`.
     *
     * @param conn active transaction connection
     * @param productName name of the product
     * @param deductQty quantity to subtract
     * @return true if stock was successfully deducted, false if insufficient stock or product not found
     * @throws SQLException on database failure
     */
    public boolean deductStockInTransaction(Connection conn, String productName, int deductQty) throws SQLException {
        if (deductQty <= 0) {
            return false;
        }

        String sql = "UPDATE products SET stock_qty = stock_qty - ? WHERE LOWER(product_name) = LOWER(?) AND stock_qty >= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deductQty);
            stmt.setString(2, productName.trim());
            stmt.setInt(3, deductQty);

            int affected = stmt.executeUpdate();
            return affected > 0;
        }
    }

    /**
     * Maps the current ResultSet row to a Product instance.
     */
    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getDouble("price"),
                rs.getString("category"),
                rs.getInt("stock_qty")
        );
    }
}

