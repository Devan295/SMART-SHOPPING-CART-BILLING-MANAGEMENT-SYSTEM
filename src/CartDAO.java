import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * CartDAO manages cart persistence in the `cart_items` table.
 *
 * ARCHITECTURAL NOTE:
 * In accordance with the academic baseline schema, this cart table represents
 * a single shared prototype cart. It does not partition items per cashier session
 * or cart token. In a multi-user production environment, a `cart_id` or `session_token`
 * column would be required to isolate concurrent user carts.
 */
public class CartDAO {

    /**
     * Loads all cart items ordered by cart_item_id ascending.
     */
    public List<CartItem> getAllCartItems() throws SQLException {
        List<CartItem> items = new ArrayList<>();
        String sql = "SELECT cart_item_id, product_name, price, quantity, total FROM cart_items ORDER BY cart_item_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(new CartItem(
                        rs.getInt("cart_item_id"),
                        rs.getString("product_name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getDouble("total")
                ));
            }
        }
        return items;
    }

    /**
     * Finds a single cart item by ID.
     */
    public CartItem getCartItemById(int cartItemId) throws SQLException {
        String sql = "SELECT cart_item_id, product_name, price, quantity, total FROM cart_items WHERE cart_item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cartItemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CartItem(
                            rs.getInt("cart_item_id"),
                            rs.getString("product_name"),
                            rs.getDouble("price"),
                            rs.getInt("quantity"),
                            rs.getDouble("total")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Finds a cart item by matching product name.
     */
    public CartItem getCartItemByName(String productName) throws SQLException {
        if (productName == null) return null;
        String sql = "SELECT cart_item_id, product_name, price, quantity, total FROM cart_items WHERE LOWER(product_name) = LOWER(?) LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, productName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CartItem(
                            rs.getInt("cart_item_id"),
                            rs.getString("product_name"),
                            rs.getDouble("price"),
                            rs.getInt("quantity"),
                            rs.getDouble("total")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Adds an item to the cart table.
     * If an item with the same product name already exists, updates the quantity and total.
     * Otherwise, inserts a new row and assigns the generated cart_item_id.
     *
     * @param item the CartItem to persist
     * @return true on success
     * @throws SQLException on database error
     */
    public boolean addOrMergeItem(CartItem item) throws SQLException {
        CartItem existing = getCartItemByName(item.getProductName());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + item.getQuantity());
            existing.setPrice(item.getPrice()); // authoritative price
            existing.recalculateTotal();
            boolean updated = updateCartItem(existing);
            if (updated) {
                item.setCartItemId(existing.getCartItemId());
                item.setQuantity(existing.getQuantity());
                item.setTotal(existing.getTotal());
            }
            return updated;
        }

        String sql = "INSERT INTO cart_items (product_name, price, quantity, total) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            item.recalculateTotal();
            stmt.setString(1, item.getProductName());
            stmt.setDouble(2, item.getPrice());
            stmt.setInt(3, item.getQuantity());
            stmt.setDouble(4, item.getTotal());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.setCartItemId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Updates an existing cart item in the database.
     */
    public boolean updateCartItem(CartItem item) throws SQLException {
        String sql = "UPDATE cart_items SET product_name = ?, price = ?, quantity = ?, total = ? WHERE cart_item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            item.recalculateTotal();
            stmt.setString(1, item.getProductName());
            stmt.setDouble(2, item.getPrice());
            stmt.setInt(3, item.getQuantity());
            stmt.setDouble(4, item.getTotal());
            stmt.setInt(5, item.getCartItemId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a cart item by cart_item_id.
     */
    public boolean deleteCartItem(int cartItemId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cartItemId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Clears all items from the cart table.
     */
    public boolean clearCart() throws SQLException {
        String sql = "DELETE FROM cart_items";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.executeUpdate();
            return true;
        }
    }

    /**
     * Clears all items from the cart table inside an existing transaction connection.
     */
    public boolean clearCart(Connection conn) throws SQLException {
        String sql = "DELETE FROM cart_items";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            return true;
        }
    }
}

