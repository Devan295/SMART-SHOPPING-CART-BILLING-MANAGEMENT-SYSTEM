import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window for the Smart Shopping Cart & Billing System.
 * Implemented using pure Java AWT with JDBC database integration.
 */
public class Main extends Frame implements ActionListener {

    // DAOs and Services
    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final BillingService billingService = new BillingService(productDAO, cartDAO, invoiceDAO);

    // Login Components
    private TextField userField, passField;
    private Button loginBtn;
    private Label msgLabel;

    // Cart Dashboard Components
    private java.awt.List cartList;
    private TextField itemNameField, itemPriceField, itemQtyField, customerField;
    private Choice paymentChoice;
    private Choice productChoice;
    private Button addBtn, updateBtn, deleteBtn, clearBtn, checkoutBtn, logoutBtn;
    private Label subtotalLabel, taxLabel, grandTotalLabel, infoLabel;

    // In-memory list mapping to persisted CartItems
    private final List<CartItem> cartItemsList = new ArrayList<>();
    private List<ProductDAO.Product> availableProducts = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean isProcessingCheckout = false;

    public Main() {
        setTitle("Smart Shopping Cart & Billing System");
        setSize(780, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        showLogin();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        setVisible(true);
    }

    // ==================== 1. LOGIN SCREEN ====================
    void showLogin() {
        removeAll();
        setLayout(null);

        Label title = new Label("SMART SHOPPING CART", Label.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setBounds(150, 70, 480, 30);
        add(title);

        Label subtitle = new Label("Billing & Checkout Management System", Label.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitle.setForeground(Color.GRAY);
        subtitle.setBounds(150, 105, 480, 20);
        add(subtitle);

        Label userLabel = new Label("Username:");
        userLabel.setBounds(230, 170, 80, 25);
        add(userLabel);

        userField = new TextField();
        userField.setBounds(320, 170, 180, 25);
        add(userField);

        Label passLabel = new Label("Password:");
        passLabel.setBounds(230, 210, 80, 25);
        add(passLabel);

        passField = new TextField();
        passField.setEchoChar('*');
        passField.setBounds(320, 210, 180, 25);
        add(passField);

        loginBtn = new Button("Login to Cart");
        loginBtn.setFont(new Font("Arial", Font.BOLD, 12));
        loginBtn.setBounds(320, 260, 180, 32);
        loginBtn.addActionListener(this);
        add(loginBtn);

        msgLabel = new Label("", Label.CENTER);
        msgLabel.setForeground(Color.RED);
        msgLabel.setBounds(10, 310, 760, 25);
        add(msgLabel);

        Label hint = new Label("Default: username = admin  |  password = 1234", Label.CENTER);
        hint.setForeground(Color.GRAY);
        hint.setBounds(150, 350, 480, 20);
        add(hint);

        validate();
        repaint();
    }

    // ==================== 2. CART & BILLING DASHBOARD ====================
    void showDashboard() {
        removeAll();
        setLayout(null);

        // Header
        Label title = new Label("Smart Shopping Cart - Current Cart & Billing");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setBounds(20, 35, 450, 25);
        add(title);

        logoutBtn = new Button("Logout");
        logoutBtn.setBounds(680, 35, 70, 25);
        logoutBtn.addActionListener(this);
        add(logoutBtn);

        // Cart List (Items in cart)
        Label listHeader = new Label("No.   Item Name              Price (PHP)     Qty       Total (PHP)");
        listHeader.setFont(new Font("Monospaced", Font.BOLD, 12));
        listHeader.setBounds(20, 70, 480, 20);
        add(listHeader);

        cartList = new java.awt.List();
        cartList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        cartList.setBounds(20, 95, 480, 190);
        cartList.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                selectedIndex = cartList.getSelectedIndex();
                if (selectedIndex >= 0 && selectedIndex < cartItemsList.size()) {
                    fillFields(selectedIndex);
                }
            }
        });
        add(cartList);

        // Right side: Billing Summary Panel
        Label billHeader = new Label("BILLING SUMMARY", Label.CENTER);
        billHeader.setFont(new Font("Arial", Font.BOLD, 13));
        billHeader.setBounds(520, 75, 230, 20);
        add(billHeader);

        Label custLabel = new Label("Customer Name:");
        custLabel.setBounds(520, 105, 100, 20);
        add(custLabel);

        customerField = new TextField("Walk-in Customer");
        customerField.setBounds(520, 128, 230, 22);
        add(customerField);

        Label payLabel = new Label("Payment Method:");
        payLabel.setBounds(520, 158, 110, 20);
        add(payLabel);

        paymentChoice = new Choice();
        paymentChoice.add("Cash");
        paymentChoice.add("GCash / E-Wallet");
        paymentChoice.add("Credit/Debit Card");
        paymentChoice.setBounds(520, 180, 230, 22);
        add(paymentChoice);

        subtotalLabel = new Label("Subtotal: PHP 0.00");
        subtotalLabel.setBounds(520, 215, 230, 20);
        add(subtotalLabel);

        taxLabel = new Label("Tax (12% VAT): PHP 0.00");
        taxLabel.setBounds(520, 238, 230, 20);
        add(taxLabel);

        grandTotalLabel = new Label("Grand Total: PHP 0.00");
        grandTotalLabel.setFont(new Font("Arial", Font.BOLD, 13));
        grandTotalLabel.setForeground(new Color(0, 100, 0));
        grandTotalLabel.setBounds(520, 262, 230, 22);
        add(grandTotalLabel);

        // Bottom Form for Adding / Editing Items
        int y = 300;
        Label itemSection = new Label("Item Details (Select to Edit / Fill to Add):");
        itemSection.setFont(new Font("Arial", Font.BOLD, 12));
        itemSection.setBounds(20, y, 280, 20);
        add(itemSection);

        Label quickSelectLabel = new Label("Select Product:");
        quickSelectLabel.setBounds(310, y, 95, 20);
        add(quickSelectLabel);

        productChoice = new Choice();
        productChoice.setBounds(410, y, 340, 22);
        productChoice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                onProductSelectedFromChoice();
            }
        });
        add(productChoice);

        y = 330;
        add(makeLabel("Item Name:", 20, y));
        itemNameField = new TextField();
        itemNameField.setBounds(100, y, 160, 22);
        add(itemNameField);

        add(makeLabel("Price (PHP):", 275, y));
        itemPriceField = new TextField();
        itemPriceField.setBounds(355, y, 75, 22);
        itemPriceField.setEditable(false); // Protect against manual price tampering
        add(itemPriceField);

        Label qtyLabel = new Label("Quantity:");
        qtyLabel.setBounds(440, y, 65, 22);
        add(qtyLabel);

        itemQtyField = new TextField("1");
        itemQtyField.setBounds(510, y, 75, 22);
        add(itemQtyField);

        // Action Buttons
        y = 375;
        addBtn = new Button("Add to Cart");
        addBtn.setBounds(50, y, 100, 30);
        addBtn.addActionListener(this);
        add(addBtn);

        updateBtn = new Button("Update Item");
        updateBtn.setBounds(160, y, 100, 30);
        updateBtn.addActionListener(this);
        add(updateBtn);

        deleteBtn = new Button("Remove Item");
        deleteBtn.setBounds(270, y, 100, 30);
        deleteBtn.addActionListener(this);
        add(deleteBtn);

        clearBtn = new Button("Clear Form");
        clearBtn.setBounds(380, y, 90, 30);
        clearBtn.addActionListener(this);
        add(clearBtn);

        checkoutBtn = new Button("Generate Bill / Checkout");
        checkoutBtn.setFont(new Font("Arial", Font.BOLD, 12));
        checkoutBtn.setBounds(520, y, 230, 30);
        checkoutBtn.addActionListener(this);
        add(checkoutBtn);

        // Status bar
        infoLabel = new Label("Ready", Label.CENTER);
        infoLabel.setBounds(20, 480, 730, 25);
        add(infoLabel);

        loadProductsIntoChoice();
        loadCart();
        validate();
        repaint();
    }

    Label makeLabel(String text, int x, int y) {
        Label l = new Label(text);
        l.setBounds(x, y, 80, 22);
        return l;
    }

    // ==================== HELPER METHODS ====================

    private void loadProductsIntoChoice() {
        productChoice.removeAll();
        productChoice.add("-- Select from Available Products --");
        try {
            availableProducts = productDAO.getAllProducts();
            for (ProductDAO.Product p : availableProducts) {
                productChoice.add(p.getProductName() + " | PHP " + String.format("%.2f", p.getPrice()) + " (Stock: " + p.getStockQty() + ")");
            }
        } catch (SQLException e) {
            infoLabel.setText("Failed to load products: " + e.getMessage());
        }
    }

    private void onProductSelectedFromChoice() {
        int idx = productChoice.getSelectedIndex();
        if (idx > 0 && (idx - 1) < availableProducts.size()) {
            ProductDAO.Product prod = availableProducts.get(idx - 1);
            itemNameField.setText(prod.getProductName());
            itemPriceField.setText(String.format("%.2f", prod.getPrice()));
            if (itemQtyField.getText().trim().isEmpty() || "0".equals(itemQtyField.getText().trim())) {
                itemQtyField.setText("1");
            }
            infoLabel.setText("Selected '" + prod.getProductName() + "' (Available Stock: " + prod.getStockQty() + ")");
        }
    }

    void loadCart() {
        cartList.removeAll();
        cartItemsList.clear();

        try {
            List<CartItem> items = cartDAO.getAllCartItems();
            cartItemsList.addAll(items);

            for (int i = 0; i < cartItemsList.size(); i++) {
                CartItem item = cartItemsList.get(i);
                cartList.add(item.toListRow(i + 1));
            }

            BigDecimal subtotal = billingService.calculateSubtotal(cartItemsList);
            BigDecimal tax = billingService.calculateTax(subtotal);
            BigDecimal grandTotal = billingService.calculateGrandTotal(subtotal, tax);

            subtotalLabel.setText(String.format("Subtotal: PHP %.2f", subtotal));
            taxLabel.setText(String.format("Tax (12%% VAT): PHP %.2f", tax));
            grandTotalLabel.setText(String.format("Grand Total: PHP %.2f", grandTotal));

        } catch (SQLException e) {
            infoLabel.setText("Database error loading cart: " + e.getMessage());
        }
    }

    void fillFields(int index) {
        if (index >= 0 && index < cartItemsList.size()) {
            CartItem item = cartItemsList.get(index);
            itemNameField.setText(item.getProductName());
            itemPriceField.setText(String.format("%.2f", item.getPrice()));
            itemQtyField.setText(String.valueOf(item.getQuantity()));
        }
    }

    void clearFields() {
        itemNameField.setText("");
        itemPriceField.setText("");
        itemQtyField.setText("1");
        selectedIndex = -1;
        if (productChoice != null && productChoice.getItemCount() > 0) {
            productChoice.select(0);
        }
    }

    // ==================== ACTIONS ====================
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        // 1. LOGIN
        if (src == loginBtn) {
            handleLogin();
            return;
        }

        // 2. LOGOUT
        if (src == logoutBtn) {
            clearFields();
            showLogin();
            return;
        }

        // 3. ADD ITEM TO CART
        if (src == addBtn) {
            handleAddToCart();
            return;
        }

        // 4. UPDATE ITEM
        if (src == updateBtn) {
            handleUpdateItem();
            return;
        }

        // 5. DELETE ITEM
        if (src == deleteBtn) {
            handleDeleteItem();
            return;
        }

        // 6. CLEAR FORM
        if (src == clearBtn) {
            clearFields();
            infoLabel.setText("Form cleared.");
            return;
        }

        // 7. CHECKOUT / BILL
        if (src == checkoutBtn) {
            handleCheckout();
        }
    }

    private void handleLogin() {
        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            msgLabel.setText("Please enter both username and password!");
            return;
        }

        loginBtn.setEnabled(false);
        try {
            UserDAO.User authenticatedUser = userDAO.authenticate(user, pass);
            if (authenticatedUser != null) {
                msgLabel.setText("");
                showDashboard();
            } else {
                msgLabel.setText("Invalid username or password!");
            }
        } catch (SQLException ex) {
            msgLabel.setText(DBConnection.interpretSqlException(ex));
        } finally {
            loginBtn.setEnabled(true);
        }
    }

    private void handleAddToCart() {
        String name = itemNameField.getText().trim();
        String qtyStr = itemQtyField.getText().trim();

        if (name.isEmpty() || qtyStr.isEmpty()) {
            infoLabel.setText("Please select or enter an Item Name and Quantity.");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
            if (qty <= 0) {
                infoLabel.setText("Quantity must be greater than zero!");
                return;
            }
        } catch (NumberFormatException ex) {
            infoLabel.setText("Quantity must be a valid whole number!");
            return;
        }

        try {
            // Verify against database product inventory
            ProductDAO.Product prod = productDAO.getProductByName(name);
            if (prod == null) {
                infoLabel.setText("Product '" + name + "' not found in inventory! Please select from list.");
                return;
            }

            // Check if stock is sufficient
            int currentStock = prod.getStockQty();
            CartItem existingInCart = cartDAO.getCartItemByName(prod.getProductName());
            int totalReqQty = qty + (existingInCart != null ? existingInCart.getQuantity() : 0);

            if (totalReqQty > currentStock) {
                infoLabel.setText(String.format("Insufficient stock for '%s'! Available: %d, Requested: %d",
                        prod.getProductName(), currentStock, totalReqQty));
                return;
            }

            // Persist item with authoritative database price
            CartItem newItem = new CartItem(prod.getProductName(), prod.getPrice(), qty);
            boolean saved = cartDAO.addOrMergeItem(newItem);

            if (saved) {
                loadCart();
                clearFields();
                infoLabel.setText("Item '" + prod.getProductName() + "' added to cart.");
            } else {
                infoLabel.setText("Failed to save item to cart.");
            }

        } catch (SQLException ex) {
            infoLabel.setText("Database error: " + ex.getMessage());
        }
    }

    private void handleUpdateItem() {
        if (selectedIndex < 0 || selectedIndex >= cartItemsList.size()) {
            infoLabel.setText("Please select an item from the cart first.");
            return;
        }

        String qtyStr = itemQtyField.getText().trim();
        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
            if (qty <= 0) {
                infoLabel.setText("Quantity must be greater than zero!");
                return;
            }
        } catch (NumberFormatException ex) {
            infoLabel.setText("Quantity must be a valid whole number!");
            return;
        }

        CartItem itemToUpdate = cartItemsList.get(selectedIndex);

        try {
            // Check stock for the updated quantity
            ProductDAO.Product prod = productDAO.getProductByName(itemToUpdate.getProductName());
            if (prod == null) {
                infoLabel.setText("Product no longer found in inventory!");
                return;
            }

            if (qty > prod.getStockQty()) {
                infoLabel.setText(String.format("Insufficient stock for '%s'! Available: %d, Requested: %d",
                        prod.getProductName(), prod.getStockQty(), qty));
                return;
            }

            itemToUpdate.setPrice(prod.getPrice()); // Always authoritative
            itemToUpdate.setQuantity(qty);
            itemToUpdate.recalculateTotal();

            boolean updated = cartDAO.updateCartItem(itemToUpdate);
            if (updated) {
                loadCart();
                clearFields();
                infoLabel.setText("Item '" + itemToUpdate.getProductName() + "' updated successfully.");
            } else {
                infoLabel.setText("Failed to update cart item in database.");
            }

        } catch (SQLException ex) {
            infoLabel.setText("Database error: " + ex.getMessage());
        }
    }

    private void handleDeleteItem() {
        if (selectedIndex < 0 || selectedIndex >= cartItemsList.size()) {
            infoLabel.setText("Please select an item to remove.");
            return;
        }

        CartItem itemToDelete = cartItemsList.get(selectedIndex);

        try {
            boolean deleted = cartDAO.deleteCartItem(itemToDelete.getCartItemId());
            if (deleted) {
                loadCart();
                clearFields();
                infoLabel.setText("Item removed from cart.");
            } else {
                infoLabel.setText("Failed to remove item from database.");
            }
        } catch (SQLException ex) {
            infoLabel.setText("Database error: " + ex.getMessage());
        }
    }

    private void handleCheckout() {
        if (isProcessingCheckout) {
            return; // Prevent duplicate concurrent checkout submissions
        }

        if (cartItemsList.isEmpty()) {
            infoLabel.setText("Cart is empty! Add items first.");
            return;
        }

        String customer = customerField.getText().trim();
        String payment = paymentChoice.getSelectedItem();

        isProcessingCheckout = true;
        checkoutBtn.setEnabled(false);
        infoLabel.setText("Processing transaction-safe checkout...");

        try {
            BillingService.CheckoutResult result = billingService.processCheckout(customer, payment);

            // Refresh UI state
            loadCart();
            clearFields();
            loadProductsIntoChoice(); // Refresh updated inventory levels

            infoLabel.setText("Bill generated for " + result.getCustomerName() + " (" + result.getPaymentMethod() +
                    "). Invoice #" + result.getInvoiceId() + " created!");

            // Display Receipt Dialog
            ReceiptDialog dialog = new ReceiptDialog(this, result);
            dialog.setVisible(true);

        } catch (IllegalStateException ex) {
            infoLabel.setText("Checkout validation failed: " + ex.getMessage());
        } catch (SQLException ex) {
            infoLabel.setText("Checkout error: " + DBConnection.interpretSqlException(ex));
        } finally {
            isProcessingCheckout = false;
            checkoutBtn.setEnabled(true);
        }
    }

    /**
     * AWT Dialog displaying the official invoice receipt.
     */
    static class ReceiptDialog extends Dialog implements ActionListener {
        private final Button closeBtn;

        public ReceiptDialog(Frame parent, BillingService.CheckoutResult result) {
            super(parent, "Invoice Receipt #" + result.getInvoiceId(), true);
            setSize(480, 460);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(10, 10));

            TextArea textArea = new TextArea(result.generateReceiptText(), 18, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setEditable(false);
            add(textArea, BorderLayout.CENTER);

            Panel bottomPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
            closeBtn = new Button("Close Receipt");
            closeBtn.addActionListener(this);
            bottomPanel.add(closeBtn);
            add(bottomPanel, BorderLayout.SOUTH);

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == closeBtn) {
                dispose();
            }
        }
    }

    public static void main(String[] args) {
        new Main();
    }
}

