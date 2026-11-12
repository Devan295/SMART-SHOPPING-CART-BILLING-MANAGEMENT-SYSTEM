import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Model class representing an item in the shopping cart,
 * mapping directly to the `cart_items` database table.
 */
public class CartItem {

    private int cartItemId;
    private String productName;
    private double price;
    private int quantity;
    private double total;

    public CartItem() {
    }

    public CartItem(String productName, double price, int quantity) {
        this(0, productName, price, quantity, price * quantity);
    }

    public CartItem(int cartItemId, String productName, double price, int quantity, double total) {
        this.cartItemId = cartItemId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.total = total;
    }

    public int getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(int cartItemId) {
        this.cartItemId = cartItemId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        recalculateTotal();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        recalculateTotal();
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    /**
     * Recalculates total using BigDecimal for monetary precision.
     */
    public void recalculateTotal() {
        BigDecimal p = BigDecimal.valueOf(this.price);
        BigDecimal q = BigDecimal.valueOf(this.quantity);
        this.total = p.multiply(q).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public BigDecimal getPriceAsBigDecimal() {
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalAsBigDecimal() {
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Formats the item as a row for display in the AWT List.
     * Matches layout: "No.   Item Name              Price (PHP)     Qty       Total (PHP)"
     */
    public String toListRow(int rowNumber) {
        String name = productName;
        if (name.length() > 22) {
            name = name.substring(0, 19) + "...";
        }
        return String.format("%-4d %-22s %-14.2f %-8d %-12.2f",
                rowNumber, name, price, quantity, total);
    }

    @Override
    public String toString() {
        return productName + " | " + String.format("%.2f", price) + " | " + quantity + " | " + String.format("%.2f", total);
    }
}

